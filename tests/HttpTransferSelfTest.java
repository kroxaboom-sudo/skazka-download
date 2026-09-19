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

        server.createContext("/file", exchange -> {
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
            HttpTransferClient client = new HttpTransferClient(
                    uri -> "127.0.0.1".equals(uri.getHost()),
                    () -> {},
                    (uri, status, retryAfter) -> {
                        if (status == 429 && "5".equals(retryAfter)) {
                            rejected.incrementAndGet();
                        }
                    },
                    total -> {}
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

            System.out.println("PASS: Skazka Download HTTP redirect/resume/rejection policy");
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
