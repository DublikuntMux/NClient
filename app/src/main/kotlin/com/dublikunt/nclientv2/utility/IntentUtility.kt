package com.dublikunt.nclientv2.utility

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity

object IntentUtility : Intent() {
    @JvmStatic
    fun startAnotherActivity(activity: AppCompatActivity, intent: Intent?) {
        activity.runOnUiThread { activity.startActivity(intent) }
    }

    fun endActivity(activity: AppCompatActivity) {
        activity.runOnUiThread { activity.finish() }
    }
}
