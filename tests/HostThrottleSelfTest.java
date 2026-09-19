import com.kroxaboom.skazka.download.HostThrottle;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public final class HostThrottleSelfTest {
    public static void main(String[] args) throws Exception {
        MemoryStore store = new MemoryStore();
        FakeTime time = new FakeTime(1_700_000_000_000L, 10_000L);
        HostThrottle throttle = new HostThrottle(store, time);
        URI host = URI.create("https://cdn.example.test/image");

        throttle.before(host, 1200L);
        check(time.slept == 0, "first request is immediate");

        throttle.before(host, 0L);
        check(time.slept == 1200L, "request spacing is enforced");

        throttle.rejected(host, 429, "5", 1000L);
        long persisted = throttle.backoffUntil(host);
        check(persisted == time.wallMillis() + 5000L, "429 Retry-After persisted");

        long before = time.slept;
        throttle.before(host, 0L);
        check(time.slept - before == 5000L, "persisted backoff blocks next request");
        check(throttle.backoffUntil(host) == 0L, "expired backoff removed");

        throttle.rejected(host, 503, "999", 0L);
        check(throttle.backoffUntil(host) == time.wallMillis() + 60_000L,
                "server backoff is capped at one minute");

        URI other = URI.create("https://other.example.test/file");
        throttle.before(other, 0L);
        check(time.slept == before + 5000L, "different host is independent");

        check(new HostThrottle(store).backoffUntil(null) == 0L, "null URI is ignored");

        System.out.println("PASS: Skazka Download host throttle spacing/backoff policy");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class MemoryStore implements HostThrottle.BackoffStore {
        private final Map<String, Long> values = new HashMap<>();

        @Override
        public long read(String host) {
            return values.getOrDefault(host, 0L);
        }

        @Override
        public void write(String host, long wallUntilMillis) {
            values.put(host, wallUntilMillis);
        }

        @Override
        public void remove(String host) {
            values.remove(host);
        }
    }

    private static final class FakeTime implements HostThrottle.TimeSource {
        private long wall;
        private long monotonic;
        private long slept;

        private FakeTime(long wall, long monotonic) {
            this.wall = wall;
            this.monotonic = monotonic;
        }

        @Override
        public long wallMillis() {
            return wall;
        }

        @Override
        public long monotonicMillis() {
            return monotonic;
        }

        @Override
        public void sleep(long millis) {
            wall += millis;
            monotonic += millis;
            slept += millis;
        }
    }
}
