package com.dublikunt.nclient.utility;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;

public class IntentUtility extends Intent {
    public static String PACKAGE_NAME;

    public static void startAnotherActivity(@NonNull Activity activity, Intent intent) {
        activity.runOnUiThread(() -> activity.startActivity(intent));
    }

    public static void endActivity(@NonNull Activity activity) {
        activity.runOnUiThread(activity::finish);
    }
}
