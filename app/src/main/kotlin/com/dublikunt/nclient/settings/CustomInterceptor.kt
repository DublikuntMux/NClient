package com.dublikunt.nclient.settings

import com.dublikunt.nclient.settings.Global.userAgent
import com.dublikunt.nclient.utility.LogUtility.download
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class CustomInterceptor(private val logRequests: Boolean) : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request: Request = chain.request()
        if (logRequests) download("Requested url: " + request.url)
        val r: Request.Builder = request.newBuilder()
        r.addHeader("User-Agent", userAgent)
        return chain.proceed(r.build())
    }
}
