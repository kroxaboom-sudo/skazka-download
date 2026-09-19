package com.kroxaboom.skazka.download;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URI;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RU: Общая per-host политика spacing/backoff без привязки к Android storage.
 * EN: Shared per-host spacing/backoff policy without Android storage coupling.
 */
public final class HostThrottle {
    public interface BackoffStore {
        long read(String host);

        void write(String host, long wallUntilMillis);

        void remove(String host);
    }

    public interface TimeSource {
        long wallMillis();

        long monotonicMillis();

        void sleep(long millis) throws InterruptedException;
    }

    private static final TimeSource SYSTEM_TIME = new TimeSource() {
        public long wallMillis() {
            return System.currentTimeMillis();
        }

        public long monotonicMillis() {
            return System.nanoTime() / 1_000_000L;
        }

        public void sleep(long millis) throws InterruptedException {
            Thread.sleep(millis);
        }
    };

    private final BackoffStore store;
    private final TimeSource time;
    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> nextRequest = new ConcurrentHashMap<>();

    public HostThrottle(BackoffStore store) {
        this(store, SYSTEM_TIME);
    }

    public HostThrottle(BackoffStore store, TimeSource time) {
        if (store == null || time == null) {
            throw new IllegalArgumentException("Backoff store and time source are required");
        }
        this.store = store;
        this.time = time;
    }

    public long backoffUntil(URI uri) {
        String host = host(uri);
        return host.isEmpty() ? 0 : Math.max(0, store.read(host));
    }

    public void before(URI uri, long requestDelayMillis) throws IOException {
        String host = host(uri);
        if (host.isEmpty()) {
            return;
        }

        Object lock = locks.computeIfAbsent(host, key -> new Object());
        synchronized (lock) {
            while (true) {
                long wallWait = store.read(host) - time.wallMillis();
                long spacingWait = nextRequest.getOrDefault(host, 0L) - time.monotonicMillis();
                long wait = Math.max(wallWait, spacingWait);

                if (wait <= 0) {
                    if (wallWait <= 0) {
                        store.remove(host);
                    }
                    break;
                }

                if (Thread.currentThread().isInterrupted()) {
                    throw interrupted();
                }
                try {
                    time.sleep(Math.min(wait, 1000L));
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw interrupted(error);
                }
            }

            long delay = Math.max(0, requestDelayMillis);
            if (delay > 0) {
                nextRequest.put(host, time.monotonicMillis() + delay);
            }
        }
    }

    public void rejected(
            URI uri,
            int status,
            String retryAfter,
            long requestDelayMillis
    ) {
        String host = host(uri);
        if (host.isEmpty()) {
            return;
        }

        long delay = RetryAfter.fallbackMillis(status, retryAfter, time.wallMillis());
        delay = Math.max(delay, Math.max(0, requestDelayMillis));
        delay = Math.min(delay, 60_000L);

        long monotonicUntil = time.monotonicMillis() + delay;
        long wallUntil = time.wallMillis() + delay;
        Object lock = locks.computeIfAbsent(host, key -> new Object());

        synchronized (lock) {
            nextRequest.merge(host, monotonicUntil, Math::max);
            long previous = store.read(host);
            if (wallUntil > previous) {
                store.write(host, wallUntil);
            }
        }
    }

    private static String host(URI uri) {
        if (uri == null || uri.getHost() == null) {
            return "";
        }
        return uri.getHost().trim().toLowerCase(Locale.ROOT);
    }

    private static InterruptedIOException interrupted() {
        return new InterruptedIOException("Throttle wait interrupted");
    }

    private static InterruptedIOException interrupted(InterruptedException cause) {
        InterruptedIOException error = interrupted();
        error.initCause(cause);
        return error;
    }
}
