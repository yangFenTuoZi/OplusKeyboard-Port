package com.oplus.wrapper.os;

import android.content.Context;

public final class UserManager {
    private UserManager() {}

    public static android.os.UserManager get(Context context) {
        android.os.UserManager manager = context.getSystemService(android.os.UserManager.class);
        if (manager == null) {
            throw new IllegalStateException("无法取得 Android 用户服务");
        }
        return manager;
    }
}
