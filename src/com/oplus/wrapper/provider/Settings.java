package com.oplus.wrapper.provider;

import android.content.ContentResolver;
import com.oplus.wrapper.os.UserHandle;

public final class Settings {
    private Settings() {}

    public static final class System {
        private System() {}

        public static int getIntForUser(ContentResolver resolver, String name, int defaultValue,
                                        int userId) {
            if (userId != UserHandle.myUserId()) {
                throw new SecurityException("不支持读取其他用户的系统设置");
            }
            return android.provider.Settings.System.getInt(resolver, name, defaultValue);
        }
    }
}
