package com.dublikunt.nclient.settings

import android.content.Context
import android.content.SharedPreferences
import android.webkit.CookieManager
import com.dublikunt.nclient.MainActivity
import com.dublikunt.nclient.R
import com.dublikunt.nclient.api.components.Tag
import com.dublikunt.nclient.async.database.Queries
import com.dublikunt.nclient.components.CustomCookieJar
import com.dublikunt.nclient.loginapi.LoadTags
import com.dublikunt.nclient.loginapi.User
import com.dublikunt.nclient.loginapi.User.Companion.createUser
import com.dublikunt.nclient.loginapi.User.CreateUser
import com.dublikunt.nclient.settings.Global.client
import com.dublikunt.nclient.utility.LogUtility.download
import com.dublikunt.nclient.utility.Utility
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

object Login {
    const val LOGIN_COOKIE = "sessionid"
    lateinit var BASE_HTTP_URL: HttpUrl
    @JvmStatic
    var user: User? = null
        private set
    private var accountTag = false
    private var loginShared: SharedPreferences? = null
    fun initLogin(context: Context) {
        val preferences = context.getSharedPreferences("Settings", 0)
        accountTag = preferences.getBoolean(context.getString(R.string.key_use_account_tag), false)
        BASE_HTTP_URL = Utility.getBaseUrl().toHttpUrl()
    }

    fun useAccountTag(): Boolean {
        return accountTag
    }

    fun setLoginShared(loginShared: SharedPreferences?) {
        Login.loginShared = loginShared
    }

    private fun removeCookie() {
        val cookieJar = client!!.cookieJar as CustomCookieJar
        cookieJar.removeCookie(LOGIN_COOKIE)
    }

    fun removeCloudflareCookies() {
        val cookieJar = client!!.cookieJar as CustomCookieJar
        val cookies = cookieJar.loadForRequest(BASE_HTTP_URL)
        for (cookie in cookies) {
            if (cookie.name == LOGIN_COOKIE) {
                continue
            }
            cookieJar.removeCookie(cookie.name)
        }
    }

    fun logout(context: Context?) {
        val cookieJar = client!!.cookieJar as CustomCookieJar
        removeCookie()
        cookieJar.clearSession()
        updateUser(null) //remove user
        clearOnlineTags() //remove online tags
        clearWebViewCookies(context) //clear webView cookies
    }

    fun clearWebViewCookies(context: Context?) {
        try {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        } catch (ignore: Throwable) {
        } //catch InvocationTargetException randomly thrown
    }

    fun clearOnlineTags() {
        Queries.TagTable.removeAllBlacklisted()
    }

    fun clearCookies() {
        val cookieJar = client!!.cookieJar as CustomCookieJar
        cookieJar.clear()
        cookieJar.clearSession()
    }

    @JvmStatic
    fun addOnlineTag(tag: Tag) {
        Queries.TagTable.insert(tag)
        Queries.TagTable.updateBlacklistedTag(tag, true)
    }

    @JvmStatic
    fun removeOnlineTag(tag: Tag) {
        Queries.TagTable.updateBlacklistedTag(tag, false)
    }

    fun hasCookie(name: String): Boolean {
        val cookies = client!!.cookieJar.loadForRequest(
            BASE_HTTP_URL
        )
        for (c in cookies) {
            if (c.name == name) {
                return true
            }
        }
        return false
    }

    fun isLogged(context: Context?): Boolean {
        val cookies = client!!.cookieJar.loadForRequest(
            BASE_HTTP_URL
        )
        download("Cookies: $cookies")
        if (hasCookie(LOGIN_COOKIE)) {
            if (user == null) createUser(object : CreateUser {
                override fun onCreateUser(user: User?) {
                    if (user != null) {
                        LoadTags(null).start()
                        if (context is MainActivity) {
                            context.runOnUiThread {
                                context.loginItem.title =
                                    context.getString(R.string.login_formatted, user.username)
                            }
                        }
                    }
                }
            })
            return true
        }
        if (context != null) logout(context)
        return false
        //return sessionId!=null;
    }

    @JvmStatic
    fun isLogged(): Boolean {
        return isLogged(null)
    }

    fun updateUser(user: User?) {
        Login.user = user
    }

    @JvmStatic
    fun isOnlineTags(tag: Tag): Boolean {
        return Queries.TagTable.isBlackListed(tag)
    }
}
