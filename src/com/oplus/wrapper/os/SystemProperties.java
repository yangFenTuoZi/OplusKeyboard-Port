package com.oplus.wrapper.os;

public final class SystemProperties {
    private SystemProperties() {}

    public static boolean getBoolean(String key, boolean fallback) {
        return fallback;
    }

    public static int getInt(String key, int fallback) {
        return fallback;
    }
}
