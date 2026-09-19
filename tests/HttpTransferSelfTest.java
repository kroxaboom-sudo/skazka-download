import com.kroxaboom.skazka.download.HttpTransferClient;
import com.kroxaboom.skazka.download.HttpTransferRequest;
import com.kroxaboom.skazka.download.HttpTransferResult;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class HttpTransferSelfTest {
    public static void main(String[] args) throws Exception {
        byte[] complete = "abcdef".getBytes(StandardCharsets.UTF_8);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

        AtomicInteger dynamicHeaders = new AtomicInteger();
        server.createContext("/file", exchange -> {
            if ("/file".equals(exchange.getRequestHeaders().getFirst("X-Dynamic"))) {
                dynamicHeaders.incrementAndGet();
            }
            exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
            exchange.sendResponseHeaders(200, complete.length);
            exchange.getResponseBody().write(complete);
            exchange.close();
        });

        server.createContext("/redirect", exchange -> {
            exchange.getResponseHeaders().set("Location", "/file");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });

        server.createContext("/resume", exchange -> {
            String range = exchange.getRequestHeaders().getFirst("Range");
            if ("bytes=3-".equals(range)) {
                byte[] rest = "def".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
                exchange.getResponseHeaders().set("Content-Range", "bytes 3-5/6");
                exchange.sendResponseHeaders(206, rest.length);
                exchange.getResponseBody().write(rest);
            } else {
                exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
                exchange.sendResponseHeaders(200, complete.length);
                exchange.getResponseBody().write(complete);
            }
            exchange.close();
        });

        server.createContext("/busy", exchange -> {
            exchange.getResponseHeaders().set("Retry-After", "5");
            exchange.sendResponseHeaders(429, -1);
            exchange.close();
        });

        server.start();
        try {
            int port = server.getAddress().getPort();
            URI root = URI.create("http://127.0.0.1:" + port);
            AtomicInteger rejected = new AtomicInteger();
            AtomicInteger beforeOpen = new AtomicInteger();
            AtomicInteger opened = new AtomicInteger();
            AtomicInteger closed = new AtomicInteger();
            HttpTransferClient client = new HttpTransferClient(
                    uri -> "127.0.0.1".equals(uri.getHost()),
                    () -> {},
                    (uri, status, retryAfter) -> {
                        if (status == 429 && "5".equals(retryAfter)) {
                            rejected.incrementAndGet();
                        }
                    },
                    total -> {},
                    uri -> Map.of("X-Dynamic", uri.getPath()),
                    new HttpTransferClient.ConnectionLifecycle() {
                        @Override
                        public void beforeOpen(URI uri) {
                            beforeOpen.incrementAndGet();
                        }

                        @Override
                        public void opened(URI uri, java.net.HttpURLConnection connection) {
                            opened.incrementAndGet();
                        }

                        @Override
                        public void closed(URI uri, java.net.HttpURLConnection connection) {
                            closed.incrementAndGet();
                        }
                    }
            );

            Path directory = Files.createTempDirectory("skazka-http-test");
            HttpTransferResult redirect = client.download(
                    request(root.resolve("/redirect"), 3),
                    directory.resolve("redirect.bin")
            );
            check(redirect.bytes() == 6, "redirect download size");
            check(
                    Files.readString(directory.resolve("redirect.bin")).equals("abcdef"),
                    "redirect content"
            );

            Path resumed = directory.resolve("resume.bin");
            Files.writeString(resumed, "abc");
            HttpTransferResult resume = client.download(
                    request(root.resolve("/resume"), 1),
                    resumed
            );
            check(resume.resumed(), "range resume used");
            check(Files.readString(resumed).equals("abcdef"), "resumed content");

            try {
                client.download(
                        request(root.resolve("/busy"), 0),
                        directory.resolve("busy.bin")
                );
                throw new AssertionError("429 must fail");
            } catch (java.io.IOException expected) {
                check(rejected.get() == 1, "rejection callback");
            }

            check(dynamicHeaders.get() == 1, "dynamic headers use current redirect URI");
            check(beforeOpen.get() > 0, "before-open hook called");
            check(beforeOpen.get() == opened.get(), "every opened connection passed before-open");
            check(opened.get() == closed.get(), "connection lifecycle is balanced");

            new HttpTransferClient(
                    uri -> true,
                    () -> {},
                    null,
                    null
            );

            System.out.println("PASS: Skazka Download HTTP redirect/resume/hooks/rejection policy");
        } finally {
            server.stop(0);
        }
    }

    private static HttpTransferRequest request(URI uri, int redirects) {
        return new HttpTransferRequest(
                uri,
                Map.of(),
                1024,
                redirects,
                1000,
                1000,
                "application/octet-stream",
                true
        );
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
