package com.oplus.keyboard.compat;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.Signature;

import java.io.IOException;
import java.io.InputStream;

public final class OriginalCertificate {
    private OriginalCertificate() {}

    public static Signature[] getSignatures(Context context, String packageName) {
        if (!context.getPackageName().equals(packageName)) {
            try {
                return context.getPackageManager().getPackageInfo(packageName,
                        PackageManager.GET_SIGNATURES).signatures;
            } catch (PackageManager.NameNotFoundException exception) {
                return null;
            }
        }
        try (InputStream certificate = context.getAssets().open("oplus-ime-original-cert.der")) {
            return new Signature[]{new Signature(certificate.readAllBytes())};
        } catch (IOException exception) {
            throw new IllegalStateException("无法读取原版键盘的公开证书", exception);
        }
    }
}
