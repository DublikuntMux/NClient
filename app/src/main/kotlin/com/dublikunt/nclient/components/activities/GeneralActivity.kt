package com.dublikunt.nclient.components.activities

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.dublikunt.nclient.R
import com.dublikunt.nclient.settings.Global

abstract class GeneralActivity : AppCompatActivity() {
    private var isFastScrollerApplied = false
    override fun onPause() {
        if (Global.hideMultitask()) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        super.onPause()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Global.initActivity(this)
    }

    override fun onResume() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        super.onResume()
        if (!isFastScrollerApplied) {
            isFastScrollerApplied = true
            Global.applyFastScroller(findViewById(R.id.recycler))
        }
    }
}
