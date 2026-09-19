import com.kroxaboom.skazka.download.DownloadCommand;
import com.kroxaboom.skazka.download.DownloadState;
import com.kroxaboom.skazka.download.DownloadTask;
import com.kroxaboom.skazka.download.NetworkPolicy;
import com.kroxaboom.skazka.download.QueuePolicy;
import com.kroxaboom.skazka.download.RetryAfter;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public final class DownloadCoreSelfTest {
    public static void main(String[] args) {
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

        System.out.println("PASS: Skazka Download Core queue/retry/network policy");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
