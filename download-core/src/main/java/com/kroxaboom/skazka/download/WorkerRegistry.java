package com.kroxaboom.skazka.download;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * RU: Thread-safe registry только для заранее скомпилированных worker-ов.
 * Remote manifests могут выбирать ключ, но не добавлять исполняемый код.
 *
 * EN: Thread-safe registry for precompiled workers only.
 * Remote manifests may select a key, but cannot install executable code.
 */
public final class WorkerRegistry<W extends SourceWorker> {
    private final LinkedHashMap<WorkerKey, W> workers = new LinkedHashMap<>();

    public synchronized void register(W worker) {
        WorkerKey key = WorkerKey.of(worker);
        if (workers.containsKey(key)) {
            throw new IllegalStateException("Worker is already registered: " + key);
        }
        workers.put(key, worker);
    }

    public synchronized void replace(W worker) {
        WorkerKey key = WorkerKey.of(worker);
        workers.put(key, worker);
    }

    public synchronized Optional<W> find(String sourceId, String route) {
        return Optional.ofNullable(workers.get(new WorkerKey(sourceId, route)));
    }

    public synchronized boolean available(String sourceId, String route) {
        return workers.containsKey(new WorkerKey(sourceId, route));
    }

    public synchronized boolean remove(String sourceId, String route) {
        return workers.remove(new WorkerKey(sourceId, route)) != null;
    }

    public synchronized int size() {
        return workers.size();
    }

    public synchronized List<WorkerKey> keys() {
        return List.copyOf(workers.keySet());
    }

    public synchronized Map<WorkerKey, W> snapshot() {
        return Map.copyOf(workers);
    }
}
