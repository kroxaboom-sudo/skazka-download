package com.kroxaboom.skazka.download;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class HttpTransferRequest {
    private final URI uri;
    private final Map<String, String> headers;
    private final long maxBytes;
    private final int maxRedirects;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;
    private final String mimePrefix;
    private final boolean resume;

    public HttpTransferRequest(URI uri, Map<String, String> headers, long maxBytes, int maxRedirects,
            int connectTimeoutMillis, int readTimeoutMillis, String mimePrefix, boolean resume) {
        if (uri == null || !uri.isAbsolute()) throw new IllegalArgumentException("Absolute URI is required");
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new IllegalArgumentException("HTTP(S) URI is required");
        }
        if (maxBytes <= 0) throw new IllegalArgumentException("maxBytes must be positive");
        if (maxRedirects < 0 || maxRedirects > 10) {
            throw new IllegalArgumentException("maxRedirects must be 0..10");
        }
        if (connectTimeoutMillis < 1000 || connectTimeoutMillis > 60_000) {
            throw new IllegalArgumentException("connect timeout must be 1000..60000 ms");
        }
        if (readTimeoutMillis < 1000 || readTimeoutMillis > 120_000) {
            throw new IllegalArgumentException("read timeout must be 1000..120000 ms");
        }
        this.uri = uri;
        this.headers = headers == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(headers));
        this.maxBytes = maxBytes;
        this.maxRedirects = maxRedirects;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
        this.mimePrefix = mimePrefix == null ? "" : mimePrefix.trim().toLowerCase();
        this.resume = resume;
    }

    public URI uri() { return uri; }
    public Map<String, String> headers() { return headers; }
    public long maxBytes() { return maxBytes; }
    public int maxRedirects() { return maxRedirects; }
    public int connectTimeoutMillis() { return connectTimeoutMillis; }
    public int readTimeoutMillis() { return readTimeoutMillis; }
    public String mimePrefix() { return mimePrefix; }
    public boolean resume() { return resume; }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof HttpTransferRequest)) return false;
        HttpTransferRequest that = (HttpTransferRequest) other;
        return maxBytes == that.maxBytes && maxRedirects == that.maxRedirects
                && connectTimeoutMillis == that.connectTimeoutMillis
                && readTimeoutMillis == that.readTimeoutMillis && resume == that.resume
                && uri.equals(that.uri) && headers.equals(that.headers) && mimePrefix.equals(that.mimePrefix);
    }

    @Override public int hashCode() {
        return Objects.hash(uri, headers, maxBytes, maxRedirects,
                connectTimeoutMillis, readTimeoutMillis, mimePrefix, resume);
    }

    @Override public String toString() {
        return "HttpTransferRequest[uri=" + uri + ", headers=" + headers + ", maxBytes=" + maxBytes
                + ", maxRedirects=" + maxRedirects + ", connectTimeoutMillis=" + connectTimeoutMillis
                + ", readTimeoutMillis=" + readTimeoutMillis + ", mimePrefix=" + mimePrefix
                + ", resume=" + resume + "]";
    }
}
