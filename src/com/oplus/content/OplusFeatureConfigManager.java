package com.oplus.content;

import com.oplus.keyboard.compat.AospCompat;

public final class OplusFeatureConfigManager {
    private static final OplusFeatureConfigManager INSTANCE = new OplusFeatureConfigManager();

    private OplusFeatureConfigManager() {}

    public static OplusFeatureConfigManager getInstance() {
        return INSTANCE;
    }

    public boolean hasFeature(String feature) {
        if ("oplus.hardware.type.tablet".equals(feature)) {
            return AospCompat.getContext().getResources()
                    .getConfiguration().smallestScreenWidthDp >= 600;
        }
        return AospCompat.getContext().getPackageManager().hasSystemFeature(feature);
    }
}
