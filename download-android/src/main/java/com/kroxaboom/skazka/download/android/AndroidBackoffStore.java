package com.kroxaboom.skazka.download.android;

import android.content.Context;
import android.content.SharedPreferences;

import com.kroxaboom.skazka.download.HostThrottle;

/**
 * RU: SharedPreferences-backed persistence для HostThrottle.
 * EN: SharedPreferences-backed persistence for HostThrottle.
 */
public final class AndroidBackoffStore implements HostThrottle.BackoffStore {
    private final SharedPreferences preferences;
    private final String prefix;

    public AndroidBackoffStore(Context context, String preferencesName, String keyPrefix) {
        if (context == null) {
            throw new IllegalArgumentException("Context is required");
        }
        String name = preferencesName == null ? "" : preferencesName.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Preferences name is required");
        }
        preferences = context.getApplicationContext().getSharedPreferences(name, Context.MODE_PRIVATE);
        prefix = keyPrefix == null ? "" : keyPrefix;
    }

    @Override
    public long read(String host) {
        return preferences.getLong(prefix + host, 0L);
    }

    @Override
    public void write(String host, long wallUntilMillis) {
        preferences.edit().putLong(prefix + host, Math.max(0, wallUntilMillis)).apply();
    }

    @Override
    public void remove(String host) {
        preferences.edit().remove(prefix + host).apply();
    }
}
