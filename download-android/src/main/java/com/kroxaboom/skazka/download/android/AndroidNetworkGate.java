package com.kroxaboom.skazka.download.android;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

import com.kroxaboom.skazka.download.NetworkPolicy;

import java.io.IOException;

/**
 * RU: Android-адаптер для чистой NetworkPolicy.
 * EN: Android adapter for the pure NetworkPolicy.
 */
public final class AndroidNetworkGate {
    private AndroidNetworkGate() {}

    public static boolean allowed(Context context, boolean wifiOnly) {
        if (!wifiOnly) {
            return true;
        }
        if (context == null) {
            return false;
        }

        ConnectivityManager manager = context.getSystemService(ConnectivityManager.class);
        if (manager == null) {
            return false;
        }

        NetworkCapabilities capabilities =
                manager.getNetworkCapabilities(manager.getActiveNetwork());

        return capabilities != null && NetworkPolicy.permits(
                true,
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI),
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        );
    }

    public static void require(Context context, boolean wifiOnly) throws IOException {
        if (!allowed(context, wifiOnly)) {
            throw new IOException("Required network is not available");
        }
    }
}
