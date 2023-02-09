package com.dublikunt.nclientv2.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.CookieManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dublikunt.nclientv2.MainActivity;
import com.dublikunt.nclientv2.R;
import com.dublikunt.nclientv2.api.components.Tag;
import com.dublikunt.nclientv2.async.database.Queries;
import com.dublikunt.nclientv2.components.CustomCookieJar;
import com.dublikunt.nclientv2.loginapi.LoadTags;
import com.dublikunt.nclientv2.loginapi.User;
import com.dublikunt.nclientv2.utility.LogUtility;
import com.dublikunt.nclientv2.utility.Utility;

import java.util.List;

import okhttp3.Cookie;
import okhttp3.HttpUrl;

public class Login {
    public static final String LOGIN_COOKIE = "sessionid";
    public static HttpUrl BASE_HTTP_URL;
    private static User user;
    private static boolean accountTag;
    private static SharedPreferences loginShared;

    public static void initLogin(@NonNull Context context) {
        SharedPreferences preferences = context.getSharedPreferences("Settings", 0);
        accountTag = preferences.getBoolean(context.getString(R.string.key_use_account_tag), false);
        BASE_HTTP_URL = HttpUrl.get(Utility.getBaseUrl());
    }

    public static boolean useAccountTag() {
        return accountTag;
    }

    public static void setLoginShared(SharedPreferences loginShared) {
        Login.loginShared = loginShared;
    }

    private static void removeCookie() {
        CustomCookieJar cookieJar = (CustomCookieJar) Global.client.cookieJar();
        cookieJar.removeCookie(Login.LOGIN_COOKIE);
    }

    public static void removeCloudflareCookies() {
        CustomCookieJar cookieJar = (CustomCookieJar) Global.client.cookieJar();
        List<Cookie> cookies = cookieJar.loadForRequest(BASE_HTTP_URL);
        for (Cookie cookie : cookies) {
            if (cookie.name().equals(LOGIN_COOKIE)) {
                continue;
            }
            cookieJar.removeCookie(cookie.name());
        }
    }

    public static void logout(Context context) {
        CustomCookieJar cookieJar = (CustomCookieJar) Global.client.cookieJar();
        removeCookie();
        cookieJar.clearSession();
        updateUser(null);//remove user
        clearOnlineTags();//remove online tags
        clearWebViewCookies(context);//clear webView cookies
    }

    public static void clearWebViewCookies(Context context) {
        try {
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
        } catch (Throwable ignore) {
        }//catch InvocationTargetException randomly thrown
    }

    public static void clearOnlineTags() {
        Queries.TagTable.removeAllBlacklisted();
    }

    public static void clearCookies() {
        CustomCookieJar cookieJar = (CustomCookieJar) Global.getClient().cookieJar();
        cookieJar.clear();
        cookieJar.clearSession();
    }

    public static void addOnlineTag(Tag tag) {
        Queries.TagTable.insert(tag);
        Queries.TagTable.updateBlacklistedTag(tag, true);
    }

    public static void removeOnlineTag(Tag tag) {
        Queries.TagTable.updateBlacklistedTag(tag, false);
    }

    public static boolean hasCookie(String name) {
        List<Cookie> cookies = Global.client.cookieJar().loadForRequest(BASE_HTTP_URL);
        for (Cookie c : cookies) {
            if (c.name().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isLogged(@Nullable Context context) {
        List<Cookie> cookies = Global.client.cookieJar().loadForRequest(BASE_HTTP_URL);
        LogUtility.d("Cookies: " + cookies);
        if (hasCookie(LOGIN_COOKIE)) {
            if (user == null) User.createUser(user -> {
                if (user != null) {
                    new LoadTags(null).start();
                    if (context instanceof MainActivity) {
                        ((MainActivity) context).runOnUiThread(() -> ((MainActivity) context).loginItem.setTitle(context.getString(R.string.login_formatted, user.getUsername())));
                    }
                }
            });
            return true;
        }
        if (context != null) logout(context);
        return false;
        //return sessionId!=null;
    }

    public static boolean isLogged() {
        return isLogged(null);
    }


    public static User getUser() {
        return user;
    }

    public static void updateUser(User user) {
        Login.user = user;
    }


    public static boolean isOnlineTags(Tag tag) {
        return Queries.TagTable.isBlackListed(tag);
    }
}
