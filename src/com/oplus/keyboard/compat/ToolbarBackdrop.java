package com.oplus.keyboard.compat;

import android.content.Context;
import android.database.ContentObserver;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RecordingCanvas;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

public final class ToolbarBackdrop {
    private static final String TAG = "OplusImeToolbar";
    private ToolbarBackdrop() {}

    public static void attach(View overlay) {
        Binding binding = new Binding(overlay);
        overlay.addOnAttachStateChangeListener(binding);
        overlay.getViewTreeObserver().addOnGlobalLayoutListener(binding);
        binding.register();
    }

    private static final class Binding implements View.OnAttachStateChangeListener,
            ViewTreeObserver.OnGlobalLayoutListener, ViewTreeObserver.OnScrollChangedListener {
        final View overlay;
        final Context context;
        final ContentObserver observer;
        View source;
        View appBar;
        Drawable originalBackground;
        Drawable transparentBackground;
        BackdropDrawable backdrop;
        boolean enabled;

        Binding(View overlay) {
            this.overlay = overlay;
            context = overlay.getContext();
            observer = new ContentObserver(new Handler(Looper.getMainLooper())) {
                @Override public void onChange(boolean selfChange) { update(); }
            };
        }

        void register() {
            context.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor("system_material_blur_enable"), false, observer);
            context.getContentResolver().registerContentObserver(
                    Settings.Global.getUriFor("low_power"), false, observer);
        }

        @Override public void onGlobalLayout() {
            if (source == null || appBar == null) {
                ViewGroup coordinator = findCoordinator(overlay);
                if (coordinator != null) {
                    source = findView(coordinator, "androidx.recyclerview.widget.RecyclerView");
                    appBar = findView(coordinator, "com.google.android.material.appbar.COUIAppBarLayout");
                }
                if (source != null && appBar != null) {
                    source.getViewTreeObserver().addOnScrollChangedListener(this);
                    update();
                }
            }
            if (backdrop != null) {
                backdrop.setBounds(0, 0, overlay.getWidth(), overlay.getHeight());
                backdrop.invalidateSelf();
            }
        }

        void update() {
            if (source == null || appBar == null) return;
            PowerManager power = context.getSystemService(PowerManager.class);
            boolean next = overlay.isHardwareAccelerated() && !AospCompat.isLowAnimationLevel(-1)
                    && power != null && !power.isPowerSaveMode()
                    && Settings.System.getInt(context.getContentResolver(),
                            "system_material_blur_enable", 1) == 1;
            if (next == enabled) return;
            enabled = next;
            if (enabled) {
                originalBackground = appBar.getBackground();
                setBackgroundPreservingPadding(appBar, new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
                transparentBackground = appBar.getBackground();
                backdrop = new BackdropDrawable(overlay, source);
                backdrop.setBounds(0, 0, overlay.getWidth(), overlay.getHeight());
                overlay.getOverlay().add(backdrop);
            } else {
                removeBackdrop();
            }
        }

        private void removeBackdrop() {
            if (backdrop != null) overlay.getOverlay().remove(backdrop);
            backdrop = null;
            if (appBar != null && appBar.getBackground() == transparentBackground) {
                setBackgroundPreservingPadding(appBar, originalBackground);
            }
            originalBackground = null;
            transparentBackground = null;
        }

        @Override public void onScrollChanged() { if (backdrop != null) backdrop.invalidateSelf(); }
        @Override public void onViewAttachedToWindow(View view) {}
        @Override public void onViewDetachedFromWindow(View view) {
            context.getContentResolver().unregisterContentObserver(observer);
            if (source != null && source.getViewTreeObserver().isAlive())
                source.getViewTreeObserver().removeOnScrollChangedListener(this);
            if (overlay.getViewTreeObserver().isAlive())
                overlay.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            removeBackdrop();
            view.removeOnAttachStateChangeListener(this);
        }
    }

    private static void setBackgroundPreservingPadding(View view, Drawable background) {
        int left = view.getPaddingLeft(), top = view.getPaddingTop();
        int right = view.getPaddingRight(), bottom = view.getPaddingBottom();
        view.setBackground(background);
        view.setPadding(left, top, right, bottom);
    }

    private static ViewGroup findCoordinator(View view) {
        for (android.view.ViewParent parent = view.getParent(); parent instanceof View; parent = parent.getParent()) {
            if (parent.getClass().getName().equals("androidx.coordinatorlayout.widget.CoordinatorLayout"))
                return (ViewGroup) parent;
        }
        return null;
    }

    private static View findView(View view, String typeName) {
        for (Class<?> type = view.getClass(); type != null; type = type.getSuperclass()) {
            if (type.getName().equals(typeName)) return view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findView(group.getChildAt(i), typeName);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static final class BackdropDrawable extends Drawable {
        final View overlay;
        final View source;
        final RenderNode node = new RenderNode("COUI toolbar backdrop");
        final int[] sourceLocation = new int[2];
        final int[] overlayLocation = new int[2];
        final Paint mask = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint layerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        final TypedValue backgroundValue = new TypedValue();
        int maskHeight;
        boolean warnedSoftware;

        BackdropDrawable(View overlay, View source) {
            this.overlay = overlay;
            this.source = source;
            float radius = 12f * overlay.getResources().getDisplayMetrics().density;
            node.setRenderEffect(RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP));
            mask.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        }

        @Override public void draw(Canvas canvas) {
            int width = getBounds().width(), height = getBounds().height();
            if (width <= 0 || height <= 0) return;
            if (!canvas.isHardwareAccelerated()) {
                if (!warnedSoftware) Log.w(TAG, "软件画布不支持工具栏背景模糊");
                warnedSoftware = true;
                return;
            }
            source.getLocationOnScreen(sourceLocation);
            overlay.getLocationOnScreen(overlayLocation);
            node.setPosition(0, 0, width, height);
            RecordingCanvas recording = node.beginRecording(width, height);
            try {
                int backgroundColor = overlay.getContext().getTheme().resolveAttribute(
                        android.R.attr.colorBackground, backgroundValue, true)
                        ? backgroundValue.data : Color.TRANSPARENT;
                recording.drawColor(backgroundColor);
                recording.translate(sourceLocation[0] - overlayLocation[0],
                        sourceLocation[1] - overlayLocation[1]);
                source.draw(recording);
            } finally {
                node.endRecording();
            }
            int layer = canvas.saveLayer(0, 0, width, height, layerPaint);
            canvas.drawRenderNode(node);
            if (maskHeight != height) {
                mask.setShader(new LinearGradient(0, 0, 0, height,
                        new int[]{Color.WHITE, Color.WHITE, Color.TRANSPARENT},
                        new float[]{0f, 0.75f, 1f}, Shader.TileMode.CLAMP));
                maskHeight = height;
            }
            canvas.drawRect(0, 0, width, height, mask);
            canvas.restoreToCount(layer);
        }

        @Override public void setAlpha(int alpha) { layerPaint.setAlpha(alpha); invalidateSelf(); }
        @Override public int getAlpha() { return layerPaint.getAlpha(); }
        @Override public void setColorFilter(ColorFilter colorFilter) {
            layerPaint.setColorFilter(colorFilter);
            invalidateSelf();
        }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
