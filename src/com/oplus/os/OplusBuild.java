package com.oplus.os;

import android.os.Build;

public final class OplusBuild {
    private OplusBuild() {}

    public static final class VERSION {
        public static final int SDK_VERSION = 0;
        public static final int SDK_SUB_VERSION = 0;
        public static final String RELEASE = Build.VERSION.RELEASE;

        private VERSION() {}
    }
}
