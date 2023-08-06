package com.dublikunt.nclient.utility;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;

public class IntentUtility extends Intent {

    public static void startAnotherActivity(@NonNull Activity activity, Intent intent) {
        activity.runOnUiThread(() -> activity.startActivity(intent));
    }

}
