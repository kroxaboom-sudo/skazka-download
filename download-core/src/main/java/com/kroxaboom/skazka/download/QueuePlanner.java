package com.kroxaboom.skazka.download;

/**
 * RU: Чистый выбор следующей задачи. Порядок задаёт вызывающее приложение.
 * EN: Pure next-task selection. The calling application owns queue ordering.
 */
public final class QueuePlanner {
    private QueuePlanner() {}

    public static QueuePlan plan(Iterable<DownloadTask> orderedTasks, long now) {
        DownloadTask candidate = null;
        long earliestRetry = Long.MAX_VALUE;
        boolean allTerminal = true;

        if (orderedTasks != null) {
            for (DownloadTask task : orderedTasks) {
                if (task == null) {
                    continue;
                }

                if (candidate == null && QueuePolicy.isEligible(task, now)) {
                    candidate = task;
                }

                if (task.state() == DownloadState.RETRY
                        && task.retryAt() > now
                        && task.retryAt() < earliestRetry) {
                    earliestRetry = task.retryAt();
                }

                if (!QueuePolicy.isTerminal(task.state())) {
                    allTerminal = false;
                }
            }
        }

        return new QueuePlan(
                candidate,
                earliestRetry == Long.MAX_VALUE ? 0 : earliestRetry,
                allTerminal
        );
    }
}
