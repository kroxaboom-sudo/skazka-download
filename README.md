# Skazka Download

> RU — основной язык · EN — required second language

## RU

Общий движок загрузок для Skazka и других Android-проектов.

**Статус:** `0.1.2-preview`.

- `download-core` — state machine очереди, pause/resume/retry/cancel, retry deadline, Retry-After/backoff и Wi-Fi-only network policy.
- Persistence/recovery — `QueueStore` + `QueueRepository`: атомарный snapshot-контракт, восстановление RUNNING/VERIFYING/NETWORK после перезапуска и сохранение переходов очереди.
- HTTP transfer — безопасная докачка через Range, явные redirect limits, проверка Content-Range/MIME/размера, callback для 429/503 и запрет переноса credential-заголовков на другой origin.
- Source-worker registry — `SourceWorker` + `WorkerRegistry`: выбор заранее скомпилированного worker-а по `sourceId + route`, защита от неявной перезаписи регистрации и immutable snapshot.
- `download-android` — Android adapter для проверки активной сети.
- Core не знает о Skazka Hub, `Context`, `JSONObject`, `LocalState`, `ChapterStore`, конкретных источниках, production hosts или UI.
- Конкретное приложение передаёт URI trust policy, network/cancellation gate, заголовки/cookies, persistence adapter и прикладной интерфейс выполнения worker-а.
- Remote manifest может выбрать зарегистрированный ключ worker-а, но не может загрузить или установить исполняемый код.

Проверено на HOSTKEY для предыдущего слоя: queue/persistence/recovery self-test — PASS; HTTP redirect/resume/rejection self-test — PASS; `:download-android:assembleDebug` — PASS; `:download-android:lintDebug` — PASS. Проверка registry выполняется перед merge этой версии.

## EN

Reusable download-engine building blocks for Skazka and other Android projects.

**Status:** `0.1.2-preview`.

- `download-core` — queue state machine, pause/resume/retry/cancel, retry deadlines, Retry-After/backoff, and Wi-Fi-only network policy.
- Persistence/recovery — `QueueStore` + `QueueRepository`: atomic snapshot contract, RUNNING/VERIFYING/NETWORK recovery after restart, and persisted queue transitions.
- HTTP transfer — safe Range resume, explicit redirect limits, Content-Range/MIME/size validation, 429/503 callback, and cross-origin credential-header stripping.
- Source-worker registry — `SourceWorker` + `WorkerRegistry`: selection of precompiled workers by `sourceId + route`, protection against implicit registration replacement, and an immutable snapshot.
- `download-android` — Android adapter for active-network checks.
- Core has no dependency on Skazka Hub, `Context`, `JSONObject`, `LocalState`, `ChapterStore`, concrete sources, production hosts, or UI.
- The concrete application supplies URI trust policy, network/cancellation gate, headers/cookies, persistence adapter, and the application-specific worker execution interface.
- A remote manifest may select a registered worker key, but cannot download or install executable code.

Previously verified on HOSTKEY: queue/persistence/recovery self-test — PASS; HTTP redirect/resume/rejection self-test — PASS; `:download-android:assembleDebug` — PASS; `:download-android:lintDebug` — PASS. Registry verification runs before this version is merged.

## Coordinates / Координаты

- `com.kroxaboom.skazka:download-core:0.1.2-preview`
- `com.kroxaboom.skazka:download-android:0.1.2-preview`

See [DEVELOPMENT_RULES.md](DEVELOPMENT_RULES.md).

> A license will be selected before the first stable public release. Until then, publication of the source does not grant reuse rights.
