package com.kroxaboom.skazka.download;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * RU: Source-neutral HTTP transfer с безопасной докачкой и явной политикой URI.
 * EN: Source-neutral HTTP transfer with safe resume support and an explicit URI policy.
 */
public final class HttpTransferClient {
    private static final Set<String> SENSITIVE =
            Set.of("authorization", "cookie", "proxy-authorization");

    public interface UriPolicy {
        boolean allowed(URI uri);
    }

    public interface Gate {
        void check() throws IOException;
    }

    public interface RejectionListener {
        void rejected(URI uri, int status, String retryAfter) throws IOException;
    }

    public interface Progress {
        void bytes(long total) throws IOException;
    }

    private final UriPolicy uriPolicy;
    private final Gate gate;
    private final RejectionListener rejectionListener;
    private final Progress progress;

    public HttpTransferClient(
            UriPolicy uriPolicy,
            Gate gate,
            RejectionListener rejectionListener,
            Progress progress
    ) {
        if (uriPolicy == null || gate == null) {
            throw new IllegalArgumentException("URI policy and gate are required");
        }
        this.uriPolicy = uriPolicy;
        this.gate = gate;
        this.rejectionListener =
                rejectionListener == null ? (uri, status, retryAfter) -> {} : rejectionListener;
        this.progress = progress == null ? total -> {} : progress;
    }

    public HttpTransferResult download(HttpTransferRequest request, Path target) throws IOException {
        if (request == null || target == null) {
            throw new IllegalArgumentException("Request and target are required");
        }

        Path parent = target.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        URI initial = request.uri();
        URI current = initial;
        long existing = request.resume() && Files.isRegularFile(target) ? Files.size(target) : 0;
        if (!request.resume() && Files.exists(target)) {
            Files.delete(target);
        }
        if (existing >= request.maxBytes()) {
            Files.deleteIfExists(target);
            existing = 0;
        }

        boolean restartedFresh = false;
        int redirects = 0;

        while (true) {
            gate.check();
            if (Thread.currentThread().isInterrupted()) {
                throw new IOException("Transfer cancelled");
            }
            if (!uriPolicy.allowed(current)) {
                throw new IOException("Transfer URI rejected by policy");
            }

            HttpURLConnection connection = (HttpURLConnection) current.toURL().openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(request.connectTimeoutMillis());
            connection.setReadTimeout(request.readTimeoutMillis());
            applyHeaders(connection, request.headers(), initial, current);
            if (existing > 0) {
                connection.setRequestProperty("Range", "bytes=" + existing + "-");
            }

            try {
                gate.check();
                int status = connection.getResponseCode();

                if (status >= 300 && status < 400) {
                    if (redirects++ >= request.maxRedirects()) {
                        throw new IOException("Too many redirects");
                    }
                    String location = connection.getHeaderField("Location");
                    if (location == null || location.isBlank()) {
                        throw new IOException("Redirect without Location");
                    }
                    current = current.resolve(location.trim());
                    continue;
                }

                if (status == 429 || status == 503) {
                    rejectionListener.rejected(
                            current,
                            status,
                            connection.getHeaderField("Retry-After")
                    );
                    throw new IOException(
                            "Server temporarily rejected transfer (HTTP " + status + ")"
                    );
                }

                if (status == 416 && existing > 0 && !restartedFresh) {
                    Files.deleteIfExists(target);
                    existing = 0;
                    restartedFresh = true;
                    continue;
                }

                if (status != 200 && status != 206) {
                    throw new IOException("Unexpected HTTP status " + status);
                }

                boolean append = status == 206 && existing > 0;
                if (status == 206) {
                    long start = contentRangeStart(connection.getHeaderField("Content-Range"));
                    if (existing <= 0 || start != existing) {
                        if (existing > 0 && !restartedFresh) {
                            Files.deleteIfExists(target);
                            existing = 0;
                            restartedFresh = true;
                            continue;
                        }
                        throw new IOException("Invalid Content-Range");
                    }
                } else {
                    existing = 0;
                    append = false;
                }

                String type = connection.getContentType();
                if (!request.mimePrefix().isEmpty()) {
                    String normalized = type == null ? "" : type.toLowerCase(Locale.ROOT);
                    if (!normalized.startsWith(request.mimePrefix())) {
                        throw new IOException("Unexpected content type");
                    }
                }

                long contentLength = connection.getContentLengthLong();
                if (contentLength > 0 && existing + contentLength > request.maxBytes()) {
                    throw new IOException("Transfer exceeds size limit");
                }

                long count = existing;
                try (InputStream in = connection.getInputStream();
                     OutputStream out = append
                             ? Files.newOutputStream(
                                     target,
                                     StandardOpenOption.CREATE,
                                     StandardOpenOption.WRITE,
                                     StandardOpenOption.APPEND
                             )
                             : Files.newOutputStream(
                                     target,
                                     StandardOpenOption.CREATE,
                                     StandardOpenOption.WRITE,
                                     StandardOpenOption.TRUNCATE_EXISTING
                             )) {
                    byte[] buffer = new byte[32 * 1024];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        gate.check();
                        if (Thread.currentThread().isInterrupted()) {
                            throw new IOException("Transfer cancelled");
                        }
                        count += read;
                        if (count > request.maxBytes()) {
                            throw new IOException("Transfer exceeds size limit");
                        }
                        out.write(buffer, 0, read);
                        progress.bytes(count);
                    }
                }

                return new HttpTransferResult(
                        current,
                        status,
                        count,
                        append,
                        type == null ? "" : type
                );
            } finally {
                connection.disconnect();
            }
        }
    }

    static long contentRangeStart(String value) {
        if (value == null) {
            return -1;
        }
        String text = value.trim().toLowerCase(Locale.ROOT);
        if (!text.startsWith("bytes ")) {
            return -1;
        }
        int dash = text.indexOf('-', 6);
        if (dash < 0) {
            return -1;
        }
        try {
            return Long.parseLong(text.substring(6, dash));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static void applyHeaders(
            HttpURLConnection connection,
            Map<String, String> headers,
            URI initial,
            URI current
    ) {
        boolean sameOrigin = sameOrigin(initial, current);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();
            if (name == null || value == null || name.isBlank()) {
                continue;
            }

            // RU: Не переносим credential-заголовки на другой origin после redirect.
            // EN: Never forward credential headers to another origin after a redirect.
            if (!sameOrigin && SENSITIVE.contains(name.trim().toLowerCase(Locale.ROOT))) {
                continue;
            }
            connection.setRequestProperty(name, value);
        }
    }

    private static boolean sameOrigin(URI left, URI right) {
        return left.getScheme().equalsIgnoreCase(right.getScheme())
                && left.getHost() != null
                && left.getHost().equalsIgnoreCase(right.getHost())
                && effectivePort(left) == effectivePort(right);
    }

    private static int effectivePort(URI uri) {
        if (uri.getPort() >= 0) {
            return uri.getPort();
        }
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }
}
