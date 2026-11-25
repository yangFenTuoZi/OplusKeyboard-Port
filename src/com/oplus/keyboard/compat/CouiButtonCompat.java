package com.oplus.keyboard.compat;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class CouiButtonCompat {
    private static final String TAG = "OplusImeCoui";
    private CouiButtonCompat() {}

    public static boolean useDrawableMaterial(View view) {
        try {
            Class<?> type = view.getClass();
            while (type != null && !type.getName().equals("com.coui.appcompat.button.COUIButton")) {
                type = type.getSuperclass();
            }
            if (type == null) throw new NoSuchFieldException("COUIButton");
            Field field = type.getDeclaredField("mIsDrawMaterialEffect");
            Field drawable = type.getDeclaredField("mShadowEdgeDrawable");
            field.setAccessible(true);
            drawable.setAccessible(true);
            return field.getBoolean(view) && drawable.get(view) != null;
        } catch (ReflectiveOperationException | SecurityException exception) {
            throw new IllegalStateException("无法检查 COUI 按钮的材质模式", exception);
        }
    }

    public static void onAttached(View view) {
        if (!useDrawableMaterial(view)) return;
        Context context = view.getContext();
        final Method stateChanged;
        try {
            stateChanged = view.getClass().getMethod("onMaterialStateChange", int.class, boolean.class);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("无法更新 COUI 按钮的材质状态", exception);
        }
        Runnable update = () -> {
            PowerManager power = context.getSystemService(PowerManager.class);
            boolean enabled = power != null && !power.isPowerSaveMode()
                    && Settings.System.getInt(context.getContentResolver(),
                            "system_material_stroke_enable", 1) == 1;
            try {
                stateChanged.invoke(view, 1, enabled);
            } catch (ReflectiveOperationException exception) {
                Log.e(TAG, "无法更新 COUI 按钮的边缘效果", exception);
            }
        };
        ContentObserver observer = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override public void onChange(boolean selfChange) { update.run(); }
        };
        context.getContentResolver().registerContentObserver(
                Settings.System.getUriFor("system_material_stroke_enable"), false, observer);
        context.getContentResolver().registerContentObserver(
                Settings.Global.getUriFor("low_power"), false, observer);
        view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View attached) {}
            @Override public void onViewDetachedFromWindow(View detached) {
                context.getContentResolver().unregisterContentObserver(observer);
                detached.removeOnAttachStateChangeListener(this);
            }
        });
        update.run();
    }
}
