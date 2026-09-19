package com.kroxaboom.skazka.download;

import java.util.Objects;

/**
 * RU: Минимальное состояние задачи, необходимое самому движку очереди.
 * EN: Minimal task state required by the queue engine itself.
 */
public final class DownloadTask {
    private final String id;
    private final DownloadState state;
    private final int retries;
    private final long retryAt;
    private final long updatedAt;

    public DownloadTask(String id, DownloadState state, int retries, long retryAt, long updatedAt) {
        this.id = id == null ? "" : id.trim();
        this.state = state == null ? DownloadState.WAITING : state;
        this.retries = Math.max(0, retries);
        this.retryAt = Math.max(0, retryAt);
        this.updatedAt = Math.max(0, updatedAt);
    }

    public String id() { return id; }
    public DownloadState state() { return state; }
    public int retries() { return retries; }
    public long retryAt() { return retryAt; }
    public long updatedAt() { return updatedAt; }

    public DownloadTask withState(DownloadState next, int nextRetries, long nextRetryAt, long now) {
        return new DownloadTask(id, next, nextRetries, nextRetryAt, now);
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DownloadTask)) return false;
        DownloadTask that = (DownloadTask) other;
        return retries == that.retries && retryAt == that.retryAt && updatedAt == that.updatedAt
                && id.equals(that.id) && state == that.state;
    }

    @Override public int hashCode() { return Objects.hash(id, state, retries, retryAt, updatedAt); }

    @Override public String toString() {
        return "DownloadTask[id=" + id + ", state=" + state + ", retries=" + retries
                + ", retryAt=" + retryAt + ", updatedAt=" + updatedAt + "]";
    }
}
