package com.kroxaboom.skazka.download;

/**
 * RU: Минимальный code-owned контракт worker-а. Исполнение остаётся в прикладном интерфейсе.
 * EN: Minimal code-owned worker contract. Execution stays in an application-specific interface.
 */
public interface SourceWorker {
    String sourceId();

    String route();
}
