package com.oplus.view;

import android.view.View;

public final class OplusView {
    public OplusView(View view) {}

    public void setOverrideLightSourceGeometry(float elevation, float lightY, float lightZ,
                                               float lightRadius, float blurRadius) {}

    public void damageInParent() {}

    public static boolean setClipExpandSize(View view, int left, int top, int right, int bottom) {
        return false;
    }

    public static boolean setShadowClippingEnabled(View view, boolean enabled) {
        return false;
    }
}
