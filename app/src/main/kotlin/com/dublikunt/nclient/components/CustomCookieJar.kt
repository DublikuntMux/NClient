package com.dublikunt.nclient.components

import com.franmontiel.persistentcookiejar.ClearableCookieJar
import com.franmontiel.persistentcookiejar.cache.CookieCache
import com.franmontiel.persistentcookiejar.persistence.CookiePersistor
import okhttp3.Cookie
import okhttp3.HttpUrl

class CustomCookieJar(private val cache: CookieCache, private val persistor: CookiePersistor) :
    ClearableCookieJar {
    init {
        cache.addAll(persistor.loadAll())
    }

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cache.addAll(cookies)
        persistor.saveAll(filterPersistentCookies(cookies))
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val cookiesToRemove: MutableList<Cookie> = ArrayList()
        val validCookies: MutableList<Cookie> = ArrayList()
        val it = cache.iterator()
        while (it.hasNext()) {
            val currentCookie = it.next()
            if (isCookieExpired(currentCookie)) {
                cookiesToRemove.add(currentCookie)
                it.remove()
            } else {
                validCookies.add(currentCookie)
            }
        }
        persistor.removeAll(cookiesToRemove)
        return validCookies
    }

    @Synchronized
    override fun clearSession() {
        cache.clear()
        cache.addAll(persistor.loadAll())
    }

    @Synchronized
    override fun clear() {
        cache.clear()
        persistor.clear()
    }

    fun removeCookie(name: String) {
        val cookies = persistor.loadAll()
        for (cookie in cookies) {
            if (cookie.name == name) {
                cache.clear()
                persistor.removeAll(listOf(cookie))
            }
        }
    }

    companion object {
        private fun filterPersistentCookies(cookies: List<Cookie>): List<Cookie> {
            val persistentCookies: MutableList<Cookie> = ArrayList()
            for (cookie in cookies) {
                if (cookie.persistent) {
                    persistentCookies.add(cookie)
                }
            }
            return persistentCookies
        }

        private fun isCookieExpired(cookie: Cookie): Boolean {
            return cookie.expiresAt < System.currentTimeMillis()
        }
    }
}
