package com.kroxaboom.skazka.download;

import java.util.Locale;

/**
 * RU: Стабильный ключ worker-а: источник + маршрут.
 * EN: Stable worker key: source + route.
 */
public record WorkerKey(String sourceId, String route) {
    public WorkerKey {
        sourceId = normalizeSource(sourceId);
        route = normalizeRoute(route);
    }

    public static WorkerKey of(SourceWorker worker) {
        if (worker == null) {
            throw new IllegalArgumentException("Worker is required");
        }
        return new WorkerKey(worker.sourceId(), worker.route());
    }

    private static String normalizeSource(String value) {
        String source = value == null ? "" : value.trim();
        if (source.isEmpty()) {
            throw new IllegalArgumentException("Worker sourceId is required");
        }
        if (source.length() > 128) {
            throw new IllegalArgumentException("Worker sourceId is too long");
        }
        return source;
    }

    private static String normalizeRoute(String value) {
        String route = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (route.isEmpty()) {
            throw new IllegalArgumentException("Worker route is required");
        }
        if (route.length() > 64 || !route.matches("[A-Z][A-Z0-9_]*")) {
            throw new IllegalArgumentException("Worker route is invalid");
        }
        return route;
    }
}
