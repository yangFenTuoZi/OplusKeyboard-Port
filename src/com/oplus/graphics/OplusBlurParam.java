package com.oplus.graphics;

import android.graphics.Color;

public final class OplusBlurParam {
    private int materialTint = Color.TRANSPARENT;

    public void setBlurType(int type) {}

    public void setMaterialParams(int mode, float[] blendColors, float[] mixColors) {
        materialTint = Color.TRANSPARENT;
        if (mode != 1 || mixColors == null || mixColors.length != 4) {
            return;
        }
        for (float component : mixColors) {
            if (!(component >= 0f && component <= 1f)) {
                return;
            }
        }
        // 键盘把当前皮肤的 bgKeyboardBlur 作为 RGBA 混色传入。
        materialTint = Color.argb(mixColors[3] * 0.8f, mixColors[0], mixColors[1], mixColors[2]);
    }

    public int getMaterialTint() {
        return materialTint;
    }

    public void setSmoothCornerWeight(float weight) {}

    public void setSmoothCornerType(int type) {}
}
