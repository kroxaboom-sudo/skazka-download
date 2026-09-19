package com.kroxaboom.skazka.download;

import java.io.IOException;
import java.util.Collection;

/**
 * RU: Минимальный persistence-контракт очереди. Реализация обязана заменять снимок атомарно.
 * EN: Minimal queue persistence contract. Implementations must replace the snapshot atomically.
 */
public interface QueueStore {
    Collection<DownloadTask> load() throws IOException;

    void replace(Collection<DownloadTask> tasks) throws IOException;
}
