import com.kroxaboom.skazka.download.DownloadCommand;
import com.kroxaboom.skazka.download.DownloadState;
import com.kroxaboom.skazka.download.DownloadTask;
import com.kroxaboom.skazka.download.NetworkPolicy;
import com.kroxaboom.skazka.download.QueuePlan;
import com.kroxaboom.skazka.download.QueuePlanner;
import com.kroxaboom.skazka.download.QueuePolicy;
import com.kroxaboom.skazka.download.QueueRepository;
import com.kroxaboom.skazka.download.QueueStore;
import com.kroxaboom.skazka.download.RetryAfter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public final class DownloadCoreSelfTest {
    public static void main(String[] args) throws Exception {
        long now = 1_700_000_000_000L;
        DownloadTask waiting = new DownloadTask("task-1", DownloadState.WAITING, 0, 0, now);

        check(QueuePolicy.isActive(waiting.state()), "waiting is active");
        check(QueuePolicy.isEligible(waiting, now), "waiting is eligible");

        DownloadTask paused = QueuePolicy.apply(waiting, DownloadCommand.PAUSE, now + 1);
        check(paused.state() == DownloadState.PAUSED, "pause transition");

        DownloadTask resumed = QueuePolicy.apply(paused, DownloadCommand.RESUME, now + 2);
        check(resumed.state() == DownloadState.WAITING, "resume transition");

        DownloadTask retry = new DownloadTask(
                "task-2",
                DownloadState.RETRY,
                2,
                QueuePolicy.retryAt(2, now),
                now
        );
        check(!QueuePolicy.isEligible(retry, now + 30_000), "retry waits for deadline");
        check(QueuePolicy.isEligible(retry, now + 60_000), "retry becomes eligible");

        DownloadTask failedOnce = QueuePolicy.afterFailure(waiting, 3, now + 3);
        check(failedOnce.state() == DownloadState.RETRY, "first failure schedules retry");
        check(failedOnce.retries() == 1, "first failure increments retry count");
        check(failedOnce.retryAt() == now + 30_003L, "first failure uses retry policy");

        DownloadTask exhausted = QueuePolicy.afterFailure(
                new DownloadTask("task-3", DownloadState.RUNNING, 3, 0, now),
                3,
                now + 4
        );
        check(exhausted.state() == DownloadState.ERROR, "exhausted task becomes error");
        check(exhausted.retryAt() == 0, "exhausted task clears retry deadline");

        QueuePlan immediate = QueuePlanner.plan(List.of(
                new DownloadTask("paused-first", DownloadState.PAUSED, 0, 0, now),
                new DownloadTask("ready", DownloadState.WAITING, 0, 0, now),
                new DownloadTask("later", DownloadState.RETRY, 1, now + 90_000, now)
        ), now);
        check(immediate.hasCandidate() && immediate.candidate().id().equals("ready"),
                "planner keeps caller order and selects first eligible task");
        check(immediate.wakeAt() == now + 90_000, "planner tracks earliest future retry");
        check(!immediate.allTerminal(), "active queue is not terminal");

        QueuePlan delayed = QueuePlanner.plan(List.of(
                new DownloadTask("retry-a", DownloadState.RETRY, 1, now + 50_000, now),
                new DownloadTask("retry-b", DownloadState.RETRY, 2, now + 20_000, now)
        ), now);
        check(!delayed.hasCandidate(), "future retries are not immediately eligible");
        check(delayed.wakeAt() == now + 20_000, "planner picks earliest wakeup");

        QueuePlan terminal = QueuePlanner.plan(List.of(
                new DownloadTask("done", DownloadState.DONE, 0, 0, now),
                new DownloadTask("cancelled", DownloadState.CANCELLED, 0, 0, now),
                new DownloadTask("skipped", DownloadState.SKIPPED, 0, 0, now)
        ), now);
        check(terminal.allTerminal(), "terminal queue is detected");

        check(NetworkPolicy.permits(false, false, false, true), "unrestricted network");
        check(NetworkPolicy.permits(true, true, true, false), "validated Wi-Fi");
        check(!NetworkPolicy.permits(true, true, true, true), "cellular transport blocks Wi-Fi-only");
        check(!NetworkPolicy.permits(true, false, true, false), "unvalidated Wi-Fi");

        check(RetryAfter.millis("5", now) == 5000L, "Retry-After seconds");

        String httpDate = ZonedDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(now + 10_000),
                ZoneOffset.UTC
        ).format(DateTimeFormatter.RFC_1123_DATE_TIME);
        long parsedDate = RetryAfter.millis(httpDate, now);
        check(parsedDate >= 9000L && parsedDate <= 10_000L, "Retry-After HTTP date");

        check(RetryAfter.fallbackMillis(429, "", now) == 5000L, "429 fallback");
        check(RetryAfter.fallbackMillis(503, "", now) == 3000L, "generic rejection fallback");

        MemoryStore noRecoveryStore = new MemoryStore(List.of(
                new DownloadTask("running", DownloadState.RUNNING, 1, 123, now - 30),
                new DownloadTask("network", DownloadState.NETWORK, 0, 0, now - 20)
        ));
        QueueRepository noRecovery = QueueRepository.open(noRecoveryStore);
        check(noRecovery.get("running").orElseThrow().state() == DownloadState.RUNNING,
                "plain open preserves running state");
        check(noRecovery.get("network").orElseThrow().state() == DownloadState.NETWORK,
                "plain open preserves network state");
        check(noRecoveryStore.writes == 0, "plain open does not rewrite valid snapshot");

        MemoryStore store = new MemoryStore(List.of(
                new DownloadTask("recover", DownloadState.RUNNING, 1, 123, now - 20),
                new DownloadTask("paused", DownloadState.PAUSED, 0, 0, now - 10)
        ));
        QueueRepository repository = QueueRepository.open(store, now);
        check(repository.get("recover").orElseThrow().state() == DownloadState.WAITING,
                "running task recovered to waiting");
        check(repository.get("recover").orElseThrow().retries() == 1,
                "recovery preserves retry counter");
        check(repository.get("paused").orElseThrow().state() == DownloadState.PAUSED,
                "paused task remains paused");
        check(store.writes == 1, "recovery snapshot persisted");

        repository.apply("recover", DownloadCommand.PAUSE, now + 1);
        check(repository.get("recover").orElseThrow().state() == DownloadState.PAUSED,
                "repository persists command transition");

        repository.put(new DownloadTask("future-retry", DownloadState.RETRY, 2, now + 1000, now + 2));
        check(repository.nextEligible(now).isEmpty(), "future retry is not eligible");
        check(repository.nextEligible(now + 1000).orElseThrow().id().equals("future-retry"),
                "retry becomes eligible through repository");
        check(repository.remove("future-retry"), "repository removes task");
        check(repository.get("future-retry").isEmpty(), "removed task stays absent");

        MemoryStore batchStore = new MemoryStore(List.of(
                new DownloadTask("batch-a", DownloadState.WAITING, 0, 0, now),
                new DownloadTask("batch-b", DownloadState.WAITING, 0, 0, now)
        ));
        QueueRepository batchRepository = QueueRepository.open(batchStore);
        List<DownloadTask> batchPaused = batchRepository.applyAll(
                List.of("batch-a", "batch-b", "batch-a"),
                DownloadCommand.PAUSE,
                now + 10
        );
        check(batchPaused.size() == 2, "batch command deduplicates ids");
        check(batchPaused.stream().allMatch(task -> task.state() == DownloadState.PAUSED),
                "batch command applies to every task");
        check(batchStore.writes == 1, "batch command persists exactly once");

        MemoryStore invalidBatchStore = new MemoryStore(List.of(
                new DownloadTask("valid", DownloadState.WAITING, 0, 0, now),
                new DownloadTask("done", DownloadState.DONE, 0, 0, now)
        ));
        QueueRepository invalidBatch = QueueRepository.open(invalidBatchStore);
        try {
            invalidBatch.applyAll(
                    List.of("valid", "done"),
                    DownloadCommand.PAUSE,
                    now + 11
            );
            throw new AssertionError("invalid batch must fail atomically");
        } catch (IllegalStateException expected) {
            check(invalidBatch.get("valid").orElseThrow().state() == DownloadState.WAITING,
                    "failed batch leaves earlier task unchanged");
            check(invalidBatchStore.writes == 0, "failed batch does not persist");
        }

        System.out.println("PASS: Skazka Download Core queue/persistence/recovery/network policy");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class MemoryStore implements QueueStore {
        private List<DownloadTask> tasks;
        private int writes;

        private MemoryStore(Collection<DownloadTask> initial) {
            tasks = new ArrayList<>(initial);
        }

        @Override
        public Collection<DownloadTask> load() {
            return new ArrayList<>(tasks);
        }

        @Override
        public void replace(Collection<DownloadTask> replacement) {
            tasks = new ArrayList<>(replacement);
            writes++;
        }
    }
}
