package com.kroxaboom.skazka.download;

import java.util.EnumSet;

/**
 * RU: Чистая state machine очереди. Хранилище и UI не участвуют в принятии решения.
 * EN: Pure queue state machine. Storage and UI do not participate in transition decisions.
 */
public final class QueuePolicy {
    private static final EnumSet<DownloadState> ACTIVE = EnumSet.of(
            DownloadState.WAITING,
            DownloadState.RUNNING,
            DownloadState.VERIFYING,
            DownloadState.RETRY,
            DownloadState.NETWORK
    );

    private static final EnumSet<DownloadState> RESUMABLE = EnumSet.of(
            DownloadState.PAUSED,
            DownloadState.ERROR,
            DownloadState.NETWORK,
            DownloadState.RETRY
    );

    private QueuePolicy() {}

    public static boolean isActive(DownloadState state) {
        return state != null && ACTIVE.contains(state);
    }

    public static boolean isPending(DownloadState state) {
        return isActive(state) || state == DownloadState.PAUSED || state == DownloadState.ERROR;
    }

    public static boolean isEligible(DownloadTask task, long now) {
        if (task == null) {
            return false;
        }
        return task.state() == DownloadState.WAITING
                || task.state() == DownloadState.NETWORK
                || (task.state() == DownloadState.RETRY && task.retryAt() <= now);
    }

    public static long retryAt(int retry, long now) {
        int bounded = Math.max(1, Math.min(3, retry));
        return now + 30_000L * bounded;
    }

    public static DownloadTask apply(DownloadTask task, DownloadCommand command, long now) {
        if (task == null || command == null) {
            throw new IllegalArgumentException("Task and command are required");
        }

        return switch (command) {
            case CANCEL -> task.withState(DownloadState.CANCELLED, 0, 0, now);
            case PAUSE -> {
                if (!isActive(task.state())) {
                    throw new IllegalStateException("Only an active task can be paused");
                }
                yield task.withState(DownloadState.PAUSED, 0, 0, now);
            }
            case RESUME -> {
                if (!RESUMABLE.contains(task.state())) {
                    throw new IllegalStateException("Task cannot be resumed from " + task.state());
                }
                yield task.withState(DownloadState.WAITING, 0, 0, now);
            }
            case RETRY -> {
                if (task.state() != DownloadState.ERROR && task.state() != DownloadState.RETRY) {
                    throw new IllegalStateException("Only a failed task can be retried");
                }
                yield task.withState(DownloadState.WAITING, 0, 0, now);
            }
        };
    }
}
