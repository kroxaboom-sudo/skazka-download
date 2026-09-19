package com.kroxaboom.skazka.download;

/**
 * RU: Решение о Wi-Fi-only не зависит от Android API и легко тестируется отдельно.
 * EN: The Wi-Fi-only decision is independent from Android APIs and can be tested in isolation.
 */
public final class NetworkPolicy {
    private NetworkPolicy() {}

    public static boolean permits(
            boolean wifiOnly,
            boolean validated,
            boolean wifi,
            boolean cellular
    ) {
        return !wifiOnly || (validated && wifi && !cellular);
    }
}
