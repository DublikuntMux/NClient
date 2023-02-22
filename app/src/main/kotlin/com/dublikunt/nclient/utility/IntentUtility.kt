package com.dublikunt.nclient.utility

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity

object IntentUtility : Intent() {

    fun startAnotherActivity(activity: AppCompatActivity, intent: Intent?) {
        activity.runOnUiThread { activity.startActivity(intent) }
    }

    fun endActivity(activity: AppCompatActivity) {
        activity.runOnUiThread { activity.finish() }
    }
}
