package com.oplus.wrapper.os;

import org.lsposed.hiddenapibypass.HiddenApiBypass;
import java.lang.reflect.InvocationTargetException;

public final class UserHandle {
    public static final int USER_SYSTEM = 0;
    private UserHandle() {}

    public static int myUserId() {
        try {
            return (Integer) HiddenApiBypass.invoke(android.os.UserHandle.class, null, "myUserId");
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("无法获取当前 Android 用户", exception);
        }
    }
}
