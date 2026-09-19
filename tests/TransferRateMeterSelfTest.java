import com.kroxaboom.skazka.download.TransferRateMeter;

public final class TransferRateMeterSelfTest {
    public static void main(String[] args) {
        FakeTime time = new FakeTime();
        TransferRateMeter meter = new TransferRateMeter(time);

        meter.start("a");
        meter.add("a", 500);
        time.now = 500;
        check(meter.rateBytesPerSecond() == 0, "short samples are hidden");

        time.now = 1000;
        check(meter.rateBytesPerSecond() == 500, "one-second sample");

        meter.start("b");
        meter.add("b", 2000);
        time.now = 2000;
        check(meter.rateBytesPerSecond() == 2250, "active rates are aggregated");
        check(meter.activeCount() == 2, "active count");

        meter.end("a");
        check(meter.rateBytesPerSecond() == 2000, "ended sample is removed");

        meter.add("missing", 100);
        meter.add("b", -1);
        check(meter.rateBytesPerSecond() == 2000, "invalid additions are ignored");

        meter.start("b");
        meter.add("b", 300);
        time.now = 3000;
        check(meter.rateBytesPerSecond() == 300, "restart replaces previous sample");

        meter.end("b");
        check(meter.rateBytesPerSecond() == 0 && meter.activeCount() == 0,
                "all samples can finish");

        System.out.println("PASS: Skazka Download transfer rate meter");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class FakeTime implements TransferRateMeter.TimeSource {
        private long now;

        @Override
        public long monotonicMillis() {
            return now;
        }
    }
}
