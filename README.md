# Skazka Download

**RU:** Независимый движок загрузок для текста, изображений, аудио, видео и других ресурсов.

**EN:** Reusable download engine for text, images, audio, video and other resources.

## Что здесь будет / What belongs here

- очередь загрузок;
- pause / resume / cancel / retry;
- восстановление после завершения процесса;
- сетевые ограничения;
- storage adapters;
- единая модель прогресса и ошибок.

## Граница / Boundary

Движок не знает, что такое «тайтл», «глава» или конкретный сайт. Такие понятия принадлежат приложению или Skazka Source SDK.

## Статус / Status

Репозиторий выделен из архитектуры Skazka. Существующий Download Engine переносится сюда по частям вместе с тестами, а не копируется вслепую.

The repository has been separated from the Skazka app architecture. The existing Download Engine will move here together with tests instead of being copied blindly.
