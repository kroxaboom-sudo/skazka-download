package com.kroxaboom.skazka.download;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

public record HttpTransferRequest(
        URI uri,
        Map<String, String> headers,
        long maxBytes,
        int maxRedirects,
        int connectTimeoutMillis,
        int readTimeoutMillis,
        String mimePrefix,
        boolean resume
) {
    public HttpTransferRequest {
        if (uri == null || !uri.isAbsolute()) {
            throw new IllegalArgumentException("Absolute URI is required");
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new IllegalArgumentException("HTTP(S) URI is required");
        }
        headers = headers == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(headers));
        if (maxBytes <= 0) {
            throw new IllegalArgumentException("maxBytes must be positive");
        }
        if (maxRedirects < 0 || maxRedirects > 10) {
            throw new IllegalArgumentException("maxRedirects must be 0..10");
        }
        if (connectTimeoutMillis < 1000 || connectTimeoutMillis > 60_000) {
            throw new IllegalArgumentException("connect timeout must be 1000..60000 ms");
        }
        if (readTimeoutMillis < 1000 || readTimeoutMillis > 120_000) {
            throw new IllegalArgumentException("read timeout must be 1000..120000 ms");
        }
        mimePrefix = mimePrefix == null ? "" : mimePrefix.trim().toLowerCase();
    }
}
