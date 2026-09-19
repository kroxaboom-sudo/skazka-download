package com.kroxaboom.skazka.download;

/**
 * RU: Нормализует незавершённые состояния после перезапуска процесса.
 * EN: Normalizes interrupted states after process restart.
 */
public final class RecoveryPolicy {
    private RecoveryPolicy() {}

    public static DownloadTask recover(DownloadTask task, long now) {
        if (task == null) {
            throw new IllegalArgumentException("Task is required");
        }

        return switch (task.state()) {
            case RUNNING, VERIFYING, NETWORK ->
                    task.withState(DownloadState.WAITING, task.retries(), 0, now);
            default -> task;
        };
    }
}
