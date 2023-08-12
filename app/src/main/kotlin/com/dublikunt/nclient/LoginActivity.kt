package com.dublikunt.nclient

import android.os.Bundle
import android.view.MenuItem
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.appcompat.widget.Toolbar
import com.dublikunt.nclient.loginapi.User
import com.dublikunt.nclient.settings.Global
import com.dublikunt.nclient.settings.Login
import com.dublikunt.nclient.utility.LogUtility
import com.dublikunt.nclient.utility.Utility
import okhttp3.Cookie

class LoginActivity : GeneralActivity() {
    private lateinit var waiter: CookieWaiter
    private lateinit var webView: WebView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Global.initActivity(this);
        setContentView(R.layout.activity_login)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        webView = findViewById(R.id.webView)
        setSupportActionBar(toolbar)
        toolbar.setTitle(R.string.title_activity_login)
        assert(supportActionBar != null)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowTitleEnabled(true)
        webView.loadUrl(Utility.getBaseUrl() + "login/")
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
            var cookies: String? = ""
            while (cookies == null || !cookies.contains("sessionid")) {
                Utility.threadSleep(100)
                if (isInterrupted) return
                cookies = manager.getCookie(Utility.getBaseUrl())
            }
            LogUtility.d("Cookie string: $cookies")
            val session = fetchCookie(cookies)
            applyCookie(session)
            runOnUiThread { finish() }
        }

        private fun applyCookie(session: String) {
            val cookie = Cookie.parse(
                Login.BASE_HTTP_URL,
                "sessionid=$session; HttpOnly; Max-Age=1209600; Path=/; SameSite=Lax"
            )
            Global.client.cookieJar.saveFromResponse(Login.BASE_HTTP_URL,
                listOf(cookie) as List<Cookie>
            )
            User.createUser(null)
            finish()
        }

        private fun fetchCookie(cookies: String): String {
            var start = cookies.indexOf("sessionid")
            start = cookies.indexOf('=', start) + 1
            val end = cookies.indexOf(';', start)
            return cookies.substring(start, if (end == -1) cookies.length else end)
        }
    }
}
