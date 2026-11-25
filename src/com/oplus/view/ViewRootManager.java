package com.oplus.view;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableWrapper;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

import com.oplus.graphics.OplusBlurParam;

import org.lsposed.hiddenapibypass.HiddenApiBypass;

import java.lang.reflect.Method;

public final class ViewRootManager implements View.OnAttachStateChangeListener {
    private static final String TAG = "OplusImeBlur";
    private static boolean blurApiUnavailable;

    private final View view;
    private final GradientDrawable background;
    private final DrawableWrapper drawable;
    private Drawable blur;
    private Method applyBlurRadius;
    private Method applyColor;
    private Method applyCorners;
    private int radius;
    private Integer color;
    private int materialTint = Color.TRANSPARENT;
    private float[] corners = new float[8];

    public ViewRootManager(View view) {
        this.view = view;
        background = new GradientDrawable();
        TypedValue value = new TypedValue();
        if (view.getContext().getTheme().resolveAttribute(android.R.attr.colorBackground,
                value, true)) {
            background.setColor(value.data);
        }
        // COUI 会在 View 附着前缓存返回的 Drawable。
        drawable = new DrawableWrapper(background) {};
        view.addOnAttachStateChangeListener(this);
    }

    public Drawable getBackgroundBlurDrawable() {
        return drawable;
    }

    public void setBlurParams(OplusBlurParam params) {
        materialTint = params == null ? Color.TRANSPARENT : params.getMaterialTint();
        updateBlur();
    }

    public void setBlurRadius(int radius) {
        this.radius = Math.max(0, radius);
        updateBlur();
    }

    public void setCornerRadius(float topLeft, float topRight, float bottomRight,
                                float bottomLeft) {
        corners = new float[]{
                topLeft, topLeft, topRight, topRight,
                bottomRight, bottomRight, bottomLeft, bottomLeft
        };
        background.setCornerRadii(corners);
        updateBlur();
    }

    public void setColor(int color) {
        this.color = color;
        background.setColor(color);
        updateBlur();
    }

    @Override
    public void onViewAttachedToWindow(View view) {
        updateBlur();
    }

    @Override
    public void onViewDetachedFromWindow(View view) {
        if (blur != null) {
            blur.setBounds(0, 0, 0, 0);
            blur = null;
            drawable.setDrawable(background);
        }
    }

    private void updateBlur() {
        if (blurApiUnavailable || !view.isAttachedToWindow() || (blur == null && radius == 0)) {
            return;
        }
        try {
            if (blur == null) {
                Object root = View.class.getMethod("getViewRootImpl").invoke(view);
                if (root == null) {
                    return;
                }
                blur = (Drawable) findPlatformMethod(root.getClass(), "createBackgroundBlurDrawable")
                        .invoke(root);
                Class<?> type = blur.getClass();
                applyBlurRadius = findPlatformMethod(type, "setBlurRadius", int.class);
                applyColor = findPlatformMethod(type, "setColor", int.class);
                applyCorners = findPlatformMethod(type, "setCornerRadius", float.class, float.class,
                        float.class, float.class);
            }
            applyBlurRadius.invoke(blur, radius);
            applyColor.invoke(blur, color == null ? materialTint : color.intValue());
            applyCorners.invoke(blur, corners[0], corners[2], corners[6], corners[4]);
            if (drawable.getDrawable() != blur) {
                blur.setAlpha(drawable.getAlpha());
                drawable.setDrawable(blur);
            }
            blur.invalidateSelf();
        } catch (ReflectiveOperationException | LinkageError | SecurityException exception) {
            Log.w(TAG, "无法使用 Android 背景模糊接口", exception);
            blurApiUnavailable = true;
            onViewDetachedFromWindow(view);
        }
    }

    private static Method findPlatformMethod(Class<?> type, String name, Class<?>... parameters)
            throws NoSuchMethodException {
        try {
            return type.getMethod(name, parameters);
        } catch (NoSuchMethodException exception) {
            return HiddenApiBypass.getDeclaredMethod(type, name, parameters);
        }
    }
}
