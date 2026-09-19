package com.kroxaboom.skazka.download;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

/**
 * RU: Небольшой persistence-aware слой над чистой QueuePolicy.
 * EN: Small persistence-aware layer over the pure QueuePolicy.
 */
public final class QueueRepository {
    private final QueueStore store;
    private final LinkedHashMap<String, DownloadTask> tasks = new LinkedHashMap<>();

    private QueueRepository(QueueStore store) {
        this.store = store;
    }

    public static QueueRepository open(QueueStore store, long now) throws IOException {
        if (store == null) {
            throw new IllegalArgumentException("Store is required");
        }

        QueueRepository repository = new QueueRepository(store);
        Collection<DownloadTask> loaded = store.load();
        boolean changed = false;

        if (loaded != null) {
            for (DownloadTask raw : loaded) {
                if (raw == null || raw.id().isBlank()) {
                    changed = true;
                    continue;
                }

                DownloadTask recovered = RecoveryPolicy.recover(raw, now);
                DownloadTask previous = repository.tasks.get(recovered.id());

                if (previous == null || recovered.updatedAt() >= previous.updatedAt()) {
                    repository.tasks.put(recovered.id(), recovered);
                }
                if (previous != null || recovered != raw) {
                    changed = true;
                }
            }
        }

        if (changed) {
            repository.persist();
        }
        return repository;
    }

    public synchronized List<DownloadTask> snapshot() {
        return List.copyOf(tasks.values());
    }

    public synchronized Optional<DownloadTask> get(String id) {
        return Optional.ofNullable(tasks.get(cleanId(id)));
    }

    public synchronized DownloadTask put(DownloadTask task) throws IOException {
        requireTask(task);
        tasks.put(task.id(), task);
        persist();
        return task;
    }

    public synchronized DownloadTask apply(
            String id,
            DownloadCommand command,
            long now
    ) throws IOException {
        String key = cleanId(id);
        DownloadTask current = tasks.get(key);
        if (current == null) {
            throw new IllegalArgumentException("Unknown task: " + key);
        }

        DownloadTask next = QueuePolicy.apply(current, command, now);
        tasks.put(key, next);
        persist();
        return next;
    }

    public synchronized boolean remove(String id) throws IOException {
        String key = cleanId(id);
        if (tasks.remove(key) == null) {
            return false;
        }
        persist();
        return true;
    }

    public synchronized Optional<DownloadTask> nextEligible(long now) {
        for (DownloadTask task : tasks.values()) {
            if (QueuePolicy.isEligible(task, now)) {
                return Optional.of(task);
            }
        }
        return Optional.empty();
    }

    private void persist() throws IOException {
        store.replace(new ArrayList<>(tasks.values()));
    }

    private static void requireTask(DownloadTask task) {
        if (task == null || task.id().isBlank()) {
            throw new IllegalArgumentException("Task id is required");
        }
    }

    private static String cleanId(String id) {
        String value = id == null ? "" : id.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Task id is required");
        }
        return value;
    }
}
