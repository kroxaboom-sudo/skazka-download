# Skazka Download

> RU — основной язык · EN — required second language

## RU

Общий движок загрузок для Skazka и других Android-проектов.

**Статус:** `0.1.0-preview` — первый независимый слой уже вынесен и проверен.

- `download-core` — state machine очереди, команды pause/resume/retry/cancel, retry deadline, Retry-After/backoff и Wi-Fi-only network policy.
- `download-android` — Android adapter для проверки активной сети.
- Core не знает о Skazka Hub, `LocalState`, `ChapterStore`, конкретных источниках или UI.
- Persistence очереди, source-worker registry, HTTP transfer и recovery будут переноситься следующими слоями после стабилизации core-контрактов.

Проверено на HOSTKEY: core self-test — PASS; `:download-android:assembleDebug` — PASS; `:download-android:lintDebug` — PASS.

## EN

Reusable download-engine building blocks for Skazka and other Android projects.

**Status:** `0.1.0-preview` — the first independent layer has been extracted and verified.

- `download-core` — queue state machine, pause/resume/retry/cancel commands, retry deadlines, Retry-After/backoff, and Wi-Fi-only network policy.
- `download-android` — Android adapter for active-network checks.
- Core has no dependency on Skazka Hub, `LocalState`, `ChapterStore`, concrete sources, or UI.
- Queue persistence, source-worker registry, HTTP transfer, and recovery will be extracted as the next layers after the core contracts stabilize.

Verified on HOSTKEY: core self-test — PASS; `:download-android:assembleDebug` — PASS; `:download-android:lintDebug` — PASS.

## Coordinates / Координаты

- `com.kroxaboom.skazka:download-core:0.1.0-preview`
- `com.kroxaboom.skazka:download-android:0.1.0-preview`

See [DEVELOPMENT_RULES.md](DEVELOPMENT_RULES.md).

> A license will be selected before the first stable public release. Until then, publication of the source does not grant reuse rights.
