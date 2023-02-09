package com.dublikunt.nclientv2.utility;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

public class IntentUtility extends Intent {

    public static void startAnotherActivity(AppCompatActivity activity, Intent intent) {
        activity.runOnUiThread(() -> activity.startActivity(intent));
    }

    public static void endActivity(AppCompatActivity activity) {
        activity.runOnUiThread(activity::finish);
    }
}
