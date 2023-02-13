package com.dublikunt.nclientv2

import android.os.Bundle
import android.view.MenuItem
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import com.dublikunt.nclientv2.components.activities.GeneralActivity
import com.dublikunt.nclientv2.loginapi.User
import com.dublikunt.nclientv2.settings.Global
import com.dublikunt.nclientv2.settings.Login
import com.dublikunt.nclientv2.utility.LogUtility
import com.dublikunt.nclientv2.utility.Utility
import com.google.android.material.appbar.MaterialToolbar
import okhttp3.Cookie

/**
 * A login screen that offers login via email/password.
 */
class LoginActivity : GeneralActivity() {
    lateinit var invalid: TextView
    private lateinit var waiter: CookieWaiter
    private lateinit var webView: WebView
    var isCaptcha = false
    var captchaPassed = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        webView = findViewById(R.id.webView)
        setSupportActionBar(toolbar)
        isCaptcha = false
        captchaPassed = false
        val intent = this.intent
        if (intent != null) isCaptcha = intent.getBooleanExtra("$packageName.IS_CAPTCHA", false)
        toolbar.setTitle(if (isCaptcha) R.string.title_activity_captcha else R.string.title_activity_login)
        assert(supportActionBar != null)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowTitleEnabled(true)
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun onLoadResource(view: WebView, url: String) {
                if (url.indexOf("." + Utility.ORIGINAL_URL) > 0) captchaPassed = true
                super.onLoadResource(view, url)
            }
        }
        webSettings.loadsImagesAutomatically = true
        webSettings.userAgentString = Global.userAgent
        webView.loadUrl(Utility.getBaseUrl() + if (isCaptcha) "" else "login/")
        waiter = CookieWaiter()
        waiter.start()
    }

    override fun onDestroy() {
        if (waiter.isAlive) waiter.interrupt()
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }

    inner class CookieWaiter : Thread() {
        override fun run() {
            val manager = CookieManager.getInstance()
            var cookies: String?
            do {
                Utility.threadSleep(100)
                if (isInterrupted) {
                    LogUtility.info((if (isCaptcha) "captcha" else "login") + " interrupted")
                    return
                }
                cookies = manager.getCookie(Utility.getBaseUrl())
                if (cookies == null) continue
                val splitCookies =
                    cookies.split("; ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                for (splitCookie in splitCookies) {
                    val kv = splitCookie.split("=".toRegex(), limit = 2).toTypedArray()
                    if (kv.size == 2) {
                        applyCookie(kv[0], kv[1])
                    }
                }
            } while (!(isCaptcha && captchaPassed) && !(!isCaptcha && cookies != null && cookies.contains(
                    "sessionid="
                ))
            )
            LogUtility.info((if (isCaptcha) "captcha" else "login") + " finish")
            runOnUiThread { finish() }
        }

        private fun applyCookie(key: String, value: String) {
            val cookie = Cookie.parse(
                Login.BASE_HTTP_URL,
                "$key=$value; Max-Age=31449600; Path=/; SameSite=Lax"
            )
            Global.client?.cookieJar?.saveFromResponse(
                Login.BASE_HTTP_URL,
                listOf(cookie) as List<Cookie>
            )
            if (!isCaptcha && key == Login.LOGIN_COOKIE) User.createUser(null)
        }
    }
}
