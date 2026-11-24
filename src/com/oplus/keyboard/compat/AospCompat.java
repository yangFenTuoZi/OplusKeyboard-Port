package com.oplus.keyboard.compat;

import android.content.Context;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import android.util.Log;

public final class AospCompat {
    private AospCompat() {}

    public static boolean performHapticFeedback(View view, int effect) {
        return view.performHapticFeedback(toPlatformHapticEffect(effect));
    }

    public static boolean performHapticFeedback(View view, int effect, int flags) {
        return view.performHapticFeedback(toPlatformHapticEffect(effect), flags);
    }

    private static int toPlatformHapticEffect(int effect) {
        return effect >= 300 && effect < 400
                ? HapticFeedbackConstants.KEYBOARD_TAP : effect;
    }

    public static void vibrateKey(Vibrator vibrator, int level, Long[] durations,
                                  Integer[] amplitudes, Integer[] effectTypes) {
        if (vibrator == null || !vibrator.hasVibrator() || level < 0
                || level >= durations.length || level >= amplitudes.length
                || level >= effectTypes.length || durations[level] <= 0) {
            return;
        }
        int effectType = effectTypes[6 - level];
        int predefinedEffect;
        if (effectType == 0 || effectType == 1) {
            predefinedEffect = VibrationEffect.EFFECT_CLICK;
        } else if (effectType == 69) {
            predefinedEffect = VibrationEffect.EFFECT_HEAVY_CLICK;
        } else {
            predefinedEffect = -1;
        }
        if (predefinedEffect != -1) {
            vibrator.vibrate(VibrationEffect.createPredefined(predefinedEffect));
            return;
        }
        int amplitude = vibrator.hasAmplitudeControl()
                ? amplitudes[level] : VibrationEffect.DEFAULT_AMPLITUDE;
        vibrator.vibrate(VibrationEffect.createOneShot(durations[level], amplitude));
    }

    public static boolean isKeyVibrationEnabled(Context context) {
        android.content.SharedPreferences preferences = context.getSharedPreferences(
                "com.oplus.keyboard.restore.preference", Context.MODE_PRIVATE);
        String key = "Key_setVibration_Enabled";
        if (preferences.contains(key)) {
            return preferences.getBoolean(key, false);
        }
        return Settings.System.getInt(context.getContentResolver(),
                "input_method_key_vibration", 0) == 1;
    }

    public static void setKeyVibrationEnabled(Context context, boolean enabled) {
        context.getSharedPreferences("com.oplus.keyboard.restore.preference", Context.MODE_PRIVATE)
                .edit().putBoolean("Key_setVibration_Enabled", enabled).apply();
        try {
            context.getContentResolver().call(
                    android.net.Uri.parse("content://com.android.settings.ums_service_provider"),
                    enabled ? "open_input_method_key_vibration_switch"
                            : "close_input_method_key_vibration_switch",
                    null, null);
        } catch (RuntimeException exception) {
            android.util.Log.w("SettingsPrefs",
                    "Unable to update platform vibration switch", exception);
        }
    }

    public static void applyNavigationBar(Window window, int background, boolean darkIcons) {
        if (background == 0) {
            int drawsBackground = WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS;
            int navigationFlags = drawsBackground;
            if ((window.getAttributes().flags & navigationFlags) != drawsBackground) {
                window.setFlags(drawsBackground, navigationFlags);
            }
            if (window.isNavigationBarContrastEnforced()) {
                window.setNavigationBarContrastEnforced(false);
            }
        }
        WindowInsetsController controller = window.getInsetsController();
        if (controller != null) {
            int lightNavigationBar = WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
            int appearance = darkIcons ? lightNavigationBar : 0;
            if ((controller.getSystemBarsAppearance() & lightNavigationBar) != appearance) {
                controller.setSystemBarsAppearance(appearance, lightNavigationBar);
            }
        }
    }
}
