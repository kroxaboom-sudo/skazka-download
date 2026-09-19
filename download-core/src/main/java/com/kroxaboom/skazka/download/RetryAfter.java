package com.kroxaboom.skazka.download;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * RU: Разбирает Retry-After и ограничивает server backoff одной минутой.
 * EN: Parses Retry-After and caps server-directed backoff at one minute.
 */
public final class RetryAfter {
    private static final long MAX_DELAY_MS = 60_000L;

    private RetryAfter() {}

    public static long millis(String value, long nowMillis) {
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }

        String text = value.trim();
        try {
            long seconds = Long.parseLong(text);
            return Math.max(0, Math.min(60, seconds)) * 1000L;
        } catch (NumberFormatException ignored) {
            // RU: HTTP-date — второй допустимый формат Retry-After.
            // EN: HTTP-date is the second valid Retry-After form.
        }

        try {
            long target = ZonedDateTime.parse(
                    text,
                    DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(Locale.US)
            ).toInstant().toEpochMilli();
            return Math.max(0, Math.min(MAX_DELAY_MS, target - nowMillis));
        } catch (Exception ignored) {
            return 0;
        }
    }

    public static long fallbackMillis(int httpStatus, String retryAfter, long nowMillis) {
        long parsed = millis(retryAfter, nowMillis);
        if (parsed > 0) {
            return parsed;
        }
        return httpStatus == 429 ? 5000L : 3000L;
    }
}
