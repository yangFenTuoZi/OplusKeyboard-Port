package com.oplus.keyboard.compat;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;

import org.lsposed.hiddenapibypass.HiddenApiBypass;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

public final class OriginalCertificate {
    private OriginalCertificate() {}

    public static void install(Context context) {
        try (InputStream input = context.getAssets().open("oplus-ime-original-cert.der")) {
            Signature signature = new Signature(input.readAllBytes());
            if (!HiddenApiBypass.addHiddenApiExemptions("Landroid/app/ActivityThread;",
                    "Landroid/app/ApplicationPackageManager;", "Landroid/content/pm/IPackageManager;",
                    "Landroid/content/pm/PackageManager;")) {
                throw new IllegalStateException("无法访问包管理接口");
            }
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            Class<?> interfaceType = Class.forName("android.content.pm.IPackageManager");
            Object packageManager = context.getPackageManager();
            Field serviceField = activityThread.getDeclaredField("sPackageManager");
            Field localField = packageManager.getClass().getDeclaredField("mPM");
            serviceField.setAccessible(true);
            localField.setAccessible(true);
            Object service = serviceField.get(null);
            if (service == null || localField.get(packageManager) != service) {
                throw new IllegalStateException("包管理服务状态不一致");
            }
            Object proxy = Proxy.newProxyInstance(interfaceType.getClassLoader(),
                    new Class<?>[]{interfaceType}, (ignored, method, args) -> {
                        Object result;
                        try {
                            result = method.invoke(service, args);
                        } catch (InvocationTargetException exception) {
                            throw exception.getCause();
                        }
                        if (method.getName().equals("getPackageInfo") && args != null
                                && args.length >= 2 && context.getPackageName().equals(args[0])
                                && args[1] instanceof Number && result instanceof PackageInfo) {
                            long flags = ((Number) args[1]).longValue();
                            if ((flags & PackageManager.GET_SIGNATURES) != 0
                                    && (flags & PackageManager.GET_SIGNING_CERTIFICATES) == 0) {
                                ((PackageInfo) result).signatures = new Signature[]{signature};
                            }
                        }
                        return result;
                    });
            // Android 的包信息缓存可能已有旧结果，安装代理前先关闭本进程缓存。
            HiddenApiBypass.invoke(PackageManager.class, null, "disablePackageInfoCache");
            localField.set(packageManager, proxy);
            serviceField.set(null, proxy);
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(),
                    PackageManager.GET_SIGNATURES);
            if (info.signatures == null || info.signatures.length != 1
                    || !signature.equals(info.signatures[0])) {
                throw new IllegalStateException("包管理签名代理校验失败");
            }
        } catch (Exception exception) {
            throw new IllegalStateException("无法安装原版证书代理", exception);
        }
    }
}
