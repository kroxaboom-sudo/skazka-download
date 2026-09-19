# Skazka Download

> RU — основной язык · EN — required second language

## RU

Общий движок загрузок для Skazka и других Android-проектов.

**Статус:** `0.1.8-preview`.

- `download-core` — state machine очереди, pause/resume/retry/cancel, retry deadline, Retry-After/backoff и Wi-Fi-only network policy.
- `HostThrottle` — общий per-host request spacing и persisted backoff; storage задаётся адаптером, Android-реализация `AndroidBackoffStore` использует SharedPreferences.
- `TransferRateMeter` — потокобезопасный агрегатор скорости активных загрузок с monotonic clock и защитой от переполнения.
- `QueuePlanner` — единый выбор следующей готовой задачи, ближайшего retry wake-up и terminal-состояния очереди; `QueuePolicy.afterFailure()` — единый переход RETRY/ERROR.
- Persistence/recovery — `QueueStore` + `QueueRepository`: атомарный snapshot-контракт, восстановление RUNNING/VERIFYING/NETWORK после перезапуска и сохранение переходов очереди. `QueueRepository.open(store)` открывает актуальный снимок без recovery для обычных атомарных транзакций; `open(store, now)` сохраняет recovery-поведение. `applyAll(...)` применяет команду к набору задач атомарно и сохраняет snapshot один раз.
- HTTP transfer — безопасная докачка через Range, явные redirect limits, проверка Content-Range/MIME/размера, callback для 429/503 и запрет переноса credential-заголовков на другой origin. В 0.1.4 добавлены per-URI dynamic headers и connection lifecycle hooks для cookie/throttle/cancellation адаптеров.
- Source-worker registry — `SourceWorker` + `WorkerRegistry`: выбор заранее скомпилированного worker-а по `sourceId + route`, защита от неявной перезаписи регистрации и immutable snapshot.
- `download-android` — Android adapter для проверки активной сети.
- Core не знает о Skazka Hub, `Context`, `JSONObject`, `LocalState`, `ChapterStore`, конкретных источниках, production hosts или UI.
- Конкретное приложение передаёт URI trust policy, network/cancellation gate, заголовки/cookies, persistence adapter и прикладной интерфейс выполнения worker-а.
- Remote manifest может выбрать зарегистрированный ключ worker-а, но не может загрузить или установить исполняемый код.

Проверка 0.1.8-preview на HOSTKEY выполняется перед merge.

## EN

Reusable download-engine building blocks for Skazka and other Android projects.

**Status:** `0.1.8-preview`.

- `download-core` — queue state machine, pause/resume/retry/cancel, retry deadlines, Retry-After/backoff, and Wi-Fi-only network policy.
- `HostThrottle` — shared per-host request spacing and persisted backoff; storage is adapter-owned, with `AndroidBackoffStore` backed by SharedPreferences.
- `TransferRateMeter` — thread-safe aggregate rate meter for active transfers with a monotonic clock and overflow protection.
- `QueuePlanner` — shared selection of the next eligible task, earliest retry wake-up, and terminal queue state; `QueuePolicy.afterFailure()` — shared RETRY/ERROR transition.
- Persistence/recovery — `QueueStore` + `QueueRepository`: atomic snapshot contract, RUNNING/VERIFYING/NETWORK recovery after restart, and persisted queue transitions. `QueueRepository.open(store)` opens the current snapshot without recovery for ordinary atomic transactions; `open(store, now)` retains recovery behavior. `applyAll(...)` applies a command to a task set atomically and persists the snapshot once.
- HTTP transfer — safe Range resume, explicit redirect limits, Content-Range/MIME/size validation, 429/503 callback, and cross-origin credential-header stripping. 0.1.4 adds per-URI dynamic headers and connection lifecycle hooks for cookie/throttle/cancellation adapters.
- Source-worker registry — `SourceWorker` + `WorkerRegistry`: selection of precompiled workers by `sourceId + route`, protection against implicit registration replacement, and an immutable snapshot.
- `download-android` — Android adapter for active-network checks.
- Core has no dependency on Skazka Hub, `Context`, `JSONObject`, `LocalState`, `ChapterStore`, concrete sources, production hosts, or UI.
- The concrete application supplies URI trust policy, network/cancellation gate, headers/cookies, persistence adapter, and the application-specific worker execution interface.
- A remote manifest may select a registered worker key, but cannot download or install executable code.

HOSTKEY verification for 0.1.8-preview runs before merge.

## Coordinates / Координаты

- `com.kroxaboom.skazka:download-core:0.1.8-preview`
- `com.kroxaboom.skazka:download-android:0.1.8-preview`

See [DEVELOPMENT_RULES.md](DEVELOPMENT_RULES.md).

> A license will be selected before the first stable public release. Until then, publication of the source does not grant reuse rights.
