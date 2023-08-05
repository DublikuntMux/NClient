package com.dublikunt.nclient.settings

import com.dublikunt.nclient.settings.Global.client
import com.dublikunt.nclient.utility.CSRFGet
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.RequestBody
import java.nio.charset.Charset

class AuthRequest(
    private val referer: String,
    private val url: String,
    private val callback: Callback
) : Thread() {
    private lateinit var method: String
    private var body: RequestBody = EMPTY_BODY
    fun setMethod(method: String, body: RequestBody): AuthRequest {
        this.method = method
        this.body = body
        return this
    }

    override fun run() {
        CSRFGet({ token: String ->
            client.newCall(
                Request.Builder().url(url)
                    .addHeader("Referer", referer)
                    .addHeader("X-CSRFToken", token)
                    .addHeader("X-Requested-With", "XMLHttpRequest")
                    .method(method, body)
                    .build()
            ).enqueue(callback)
        }, referer).start()
    }

    companion object {
        val EMPTY_BODY: RequestBody = FormBody.Builder(Charset.defaultCharset()).build()
    }
}
