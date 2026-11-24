package com.oplus.osense;

import android.os.Bundle;

public final class OsenseResClient {
    private OsenseResClient() {}

    public static OsenseResClient get(Class<?> clientClass) {
        return null;
    }

    public void releaseSysResource(int eventId) {}

    public void reportKeyThread(String threadName, int threadId, int eventId, Bundle options) {}

    public void requestSysResource(int eventId, Bundle options) {}
}
