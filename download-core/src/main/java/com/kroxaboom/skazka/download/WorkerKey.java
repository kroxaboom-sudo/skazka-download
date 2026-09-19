package com.kroxaboom.skazka.download;

import java.util.Locale;
import java.util.Objects;

/**
 * RU: Стабильный ключ worker-а: источник + маршрут.
 * EN: Stable worker key: source + route.
 */
public final class WorkerKey {
    private final String sourceId;
    private final String route;

    public WorkerKey(String sourceId, String route) {
        this.sourceId = normalizeSource(sourceId);
        this.route = normalizeRoute(route);
    }

    public String sourceId() { return sourceId; }
    public String route() { return route; }

    public static WorkerKey of(SourceWorker worker) {
        if (worker == null) throw new IllegalArgumentException("Worker is required");
        return new WorkerKey(worker.sourceId(), worker.route());
    }

    private static String normalizeSource(String value) {
        String source = value == null ? "" : value.trim();
        if (source.isEmpty()) throw new IllegalArgumentException("Worker sourceId is required");
        if (source.length() > 128) throw new IllegalArgumentException("Worker sourceId is too long");
        return source;
    }

    private static String normalizeRoute(String value) {
        String route = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (route.isEmpty()) throw new IllegalArgumentException("Worker route is required");
        if (route.length() > 64 || !route.matches("[A-Z][A-Z0-9_]*")) {
            throw new IllegalArgumentException("Worker route is invalid");
        }
        return route;
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof WorkerKey)) return false;
        WorkerKey that = (WorkerKey) other;
        return sourceId.equals(that.sourceId) && route.equals(that.route);
    }

    @Override public int hashCode() { return Objects.hash(sourceId, route); }

    @Override public String toString() {
        return "WorkerKey[sourceId=" + sourceId + ", route=" + route + "]";
    }
}
