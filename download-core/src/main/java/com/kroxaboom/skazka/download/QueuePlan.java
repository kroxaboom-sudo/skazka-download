package com.kroxaboom.skazka.download;

/**
 * RU: Результат одного прохода планировщика по уже упорядоченной очереди.
 * EN: Result of one scheduler pass over an already ordered queue.
 */
public record QueuePlan(
        DownloadTask candidate,
        long wakeAt,
        boolean allTerminal
) {
    public QueuePlan {
        wakeAt = Math.max(0, wakeAt);
    }

    public boolean hasCandidate() {
        return candidate != null;
    }

    public boolean hasWakeUp() {
        return wakeAt > 0;
    }
}
