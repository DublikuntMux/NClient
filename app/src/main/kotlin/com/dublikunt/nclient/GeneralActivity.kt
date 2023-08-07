package com.dublikunt.nclient

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dublikunt.nclient.components.views.CFTokenView
import com.dublikunt.nclient.settings.Global
import java.lang.ref.WeakReference

abstract class GeneralActivity : AppCompatActivity() {
    private var isFastScrollerApplied = false
    private var tokenView: CFTokenView? = null
    private fun inflateWebView() {
        if (tokenView == null) {
            Toast.makeText(this, R.string.fetching_cloudflare_token, Toast.LENGTH_SHORT).show()
            val rootView = findViewById<View>(android.R.id.content).rootView as ViewGroup
            val v = LayoutInflater.from(this)
                .inflate(R.layout.cftoken_layout, rootView, false) as ViewGroup
            tokenView = CFTokenView(v)
            val params = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            tokenView!!.setVisibility(View.GONE)
            addContentView(v, params)
        }
    }

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
        lastActivity = WeakReference(this)
        if (!isFastScrollerApplied) {
            isFastScrollerApplied = true
            Global.applyFastScroller(findViewById(R.id.recycler))
        }
    }

    companion object {
        private var lastActivity: WeakReference<GeneralActivity>? = null

        @JvmStatic
        val lastCFView: CFTokenView?
            get() {
                if (lastActivity == null) return null
                val activity = lastActivity!!.get()
                if (activity != null) {
                    activity.runOnUiThread(Runnable { activity.inflateWebView() })
                    return activity.tokenView
                }
                return null
            }
    }
}
