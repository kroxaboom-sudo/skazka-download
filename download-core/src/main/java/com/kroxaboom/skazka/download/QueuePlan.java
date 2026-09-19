package com.kroxaboom.skazka.download;

import java.util.Objects;

/**
 * RU: Результат одного прохода планировщика по уже упорядоченной очереди.
 * EN: Result of one scheduler pass over an already ordered queue.
 */
public final class QueuePlan {
    private final DownloadTask candidate;
    private final long wakeAt;
    private final boolean allTerminal;

    public QueuePlan(DownloadTask candidate, long wakeAt, boolean allTerminal) {
        this.candidate = candidate;
        this.wakeAt = Math.max(0, wakeAt);
        this.allTerminal = allTerminal;
    }

    public DownloadTask candidate() { return candidate; }
    public long wakeAt() { return wakeAt; }
    public boolean allTerminal() { return allTerminal; }

    public boolean hasCandidate() { return candidate != null; }
    public boolean hasWakeUp() { return wakeAt > 0; }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof QueuePlan)) return false;
        QueuePlan that = (QueuePlan) other;
        return wakeAt == that.wakeAt && allTerminal == that.allTerminal
                && Objects.equals(candidate, that.candidate);
    }

    @Override public int hashCode() { return Objects.hash(candidate, wakeAt, allTerminal); }

    @Override public String toString() {
        return "QueuePlan[candidate=" + candidate + ", wakeAt=" + wakeAt
                + ", allTerminal=" + allTerminal + "]";
    }
}
