package com.oplus.view;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.View;

import com.oplus.graphics.OplusBlurParam;

public final class ViewRootManager {
    private final GradientDrawable background;

    public ViewRootManager(View view) {
        background = new GradientDrawable();
        TypedValue value = new TypedValue();
        if (view.getContext().getTheme().resolveAttribute(android.R.attr.colorBackground,
                value, true)) {
            background.setColor(value.data);
        }
    }

    public Drawable getBackgroundBlurDrawable() {
        return background;
    }

    public void setBlurParams(OplusBlurParam params) {}

    public void setBlurRadius(int radius) {}

    public void setCornerRadius(float topLeft, float topRight, float bottomRight,
                                float bottomLeft) {
        background.setCornerRadii(new float[]{
                topLeft, topLeft, topRight, topRight,
                bottomRight, bottomRight, bottomLeft, bottomLeft
        });
    }

    public void setColor(int color) {
        background.setColor(color);
    }
}
