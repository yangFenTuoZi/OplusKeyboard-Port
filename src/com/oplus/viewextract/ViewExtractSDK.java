package com.oplus.viewextract;

import android.app.assist.AssistStructure;
import android.os.Bundle;
import android.util.ArrayMap;
import android.view.autofill.AutofillId;

import java.util.ArrayList;

public final class ViewExtractSDK {
    private ViewExtractSDK() {}

    public interface OnExtractBitmapsCallback {
        void onExtractBitmaps(ArrayMap<?, ?> bitmaps, Bundle result);
    }

    public interface OnExtractViewTreeCallback {
        void onViewAssistStructureCallBack(AssistStructure structure);
    }

    public static int getViewUniqueId(AutofillId autofillId) {
        return autofillId == null ? 0 : autofillId.hashCode();
    }

    public static void requestViewExtractTree(OnExtractViewTreeCallback callback,
                                              Bundle options, String packageName) {}

    public static void getBitmapByViewViewUniqueIds(OnExtractBitmapsCallback callback,
                                                    String packageName, Bundle options,
                                                    ArrayList<Integer> viewIds,
                                                    ArrayList<AutofillId> autofillIds) {
        if (callback != null) {
            callback.onExtractBitmaps(new ArrayMap<>(), new Bundle());
        }
    }
}
