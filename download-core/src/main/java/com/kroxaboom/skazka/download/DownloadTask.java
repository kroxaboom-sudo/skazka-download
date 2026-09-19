package com.kroxaboom.skazka.download;

/**
 * RU: Минимальное состояние задачи, необходимое самому движку очереди.
 * EN: Minimal task state required by the queue engine itself.
 */
public record DownloadTask(
        String id,
        DownloadState state,
        int retries,
        long retryAt,
        long updatedAt
) {
    public DownloadTask {
        id = id == null ? "" : id.trim();
        state = state == null ? DownloadState.WAITING : state;
        retries = Math.max(0, retries);
        retryAt = Math.max(0, retryAt);
        updatedAt = Math.max(0, updatedAt);
    }

    public DownloadTask withState(DownloadState next, int nextRetries, long nextRetryAt, long now) {
        return new DownloadTask(id, next, nextRetries, nextRetryAt, now);
    }
}
