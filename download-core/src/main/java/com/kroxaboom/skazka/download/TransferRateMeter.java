package com.kroxaboom.skazka.download;

import java.util.concurrent.ConcurrentHashMap;

/**
 * RU: Потокобезопасный агрегатор средней скорости активных transfer-ов.
 * EN: Thread-safe aggregate average-rate meter for active transfers.
 */
public final class TransferRateMeter {
    public interface TimeSource {
        long monotonicMillis();
    }

    private static final TimeSource SYSTEM_TIME = () -> System.nanoTime() / 1_000_000L;
    private static final long MIN_SAMPLE_MILLIS = 1000L;

    private static final class Sample {
        private final long startedAt;
        private long bytes;

        private Sample(long startedAt) {
            this.startedAt = startedAt;
        }
    }

    private final TimeSource time;
    private final ConcurrentHashMap<String, Sample> samples = new ConcurrentHashMap<>();

    public TransferRateMeter() {
        this(SYSTEM_TIME);
    }

    public TransferRateMeter(TimeSource time) {
        if (time == null) {
            throw new IllegalArgumentException("Time source is required");
        }
        this.time = time;
    }

    public void start(String id) {
        String key = key(id);
        if (key.isEmpty()) {
            return;
        }
        samples.put(key, new Sample(time.monotonicMillis()));
    }

    public void add(String id, long bytes) {
        if (bytes <= 0) {
            return;
        }
        Sample sample = samples.get(key(id));
        if (sample == null) {
            return;
        }
        synchronized (sample) {
            sample.bytes = saturatedAdd(sample.bytes, bytes);
        }
    }

    public void end(String id) {
        String key = key(id);
        if (!key.isEmpty()) {
            samples.remove(key);
        }
    }

    public long rateBytesPerSecond() {
        long total = 0;
        long now = time.monotonicMillis();

        for (Sample sample : samples.values()) {
            synchronized (sample) {
                long elapsed = now - sample.startedAt;
                if (elapsed < MIN_SAMPLE_MILLIS || sample.bytes <= 0) {
                    continue;
                }
                long rate = sample.bytes > Long.MAX_VALUE / 1000L
                        ? Long.MAX_VALUE
                        : sample.bytes * 1000L / elapsed;
                total = saturatedAdd(total, rate);
            }
        }
        return total;
    }

    public int activeCount() {
        return samples.size();
    }

    private static String key(String id) {
        return id == null ? "" : id.trim();
    }

    private static long saturatedAdd(long left, long right) {
        if (right > 0 && left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }
}
