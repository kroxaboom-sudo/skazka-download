import com.kroxaboom.skazka.download.SourceWorker;
import com.kroxaboom.skazka.download.WorkerKey;
import com.kroxaboom.skazka.download.WorkerRegistry;

public final class WorkerRegistrySelfTest {
    public static void main(String[] args) {
        WorkerRegistry<AppWorker> registry = new WorkerRegistry<>();

        AppWorker text = new AppWorker("demo", "text_source", "text-v1");
        AppWorker mixed = new AppWorker("demo", "MIXED_SOURCE", "mixed-v1");

        registry.register(text);
        registry.register(mixed);

        check(registry.size() == 2, "two routes for one source");
        check(registry.available("demo", "TEXT_SOURCE"), "route normalized");
        check(registry.find("demo", "text_source").orElseThrow() == text, "lookup");
        check(registry.keys().get(0).equals(new WorkerKey("demo", "TEXT_SOURCE")), "stable key");

        try {
            registry.register(new AppWorker("demo", "TEXT_SOURCE", "duplicate"));
            throw new AssertionError("duplicate registration must fail");
        } catch (IllegalStateException expected) {
            check(expected.getMessage().contains("already registered"), "duplicate error");
        }

        AppWorker replacement = new AppWorker("demo", "text_source", "text-v2");
        registry.replace(replacement);
        check(
                registry.find("demo", "TEXT_SOURCE").orElseThrow().execute("x").equals("text-v2:x"),
                "explicit replacement"
        );

        var snapshot = registry.snapshot();
        check(snapshot.size() == 2, "snapshot size");
        try {
            snapshot.clear();
            throw new AssertionError("snapshot must be immutable");
        } catch (UnsupportedOperationException expected) {
            // RU: Снимок registry не должен позволять удалённому коду менять регистрацию.
            // EN: Registry snapshots must not let callers mutate registration.
        }

        check(registry.remove("demo", "mixed_source"), "remove");
        check(!registry.available("demo", "MIXED_SOURCE"), "removed route absent");

        try {
            registry.register(new AppWorker("", "TEXT_SOURCE", "bad"));
            throw new AssertionError("blank sourceId must fail");
        } catch (IllegalArgumentException expected) {
            check(expected.getMessage().contains("sourceId"), "blank source error");
        }

        try {
            registry.register(new AppWorker("demo", "../exec", "bad"));
            throw new AssertionError("invalid route must fail");
        } catch (IllegalArgumentException expected) {
            check(expected.getMessage().contains("route"), "invalid route error");
        }

        System.out.println("PASS: Skazka Download source worker registry");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class AppWorker implements SourceWorker {
        private final String sourceId;
        private final String route;
        private final String name;

        private AppWorker(String sourceId, String route, String name) {
            this.sourceId = sourceId;
            this.route = route;
            this.name = name;
        }

        @Override
        public String sourceId() {
            return sourceId;
        }

        @Override
        public String route() {
            return route;
        }

        private String execute(String request) {
            return name + ":" + request;
        }
    }
}
