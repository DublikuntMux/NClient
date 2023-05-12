package com.dublikunt.nclient.integration

import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoader.LoadData
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.dublikunt.nclient.settings.CustomInterceptor
import okhttp3.Call
import okhttp3.OkHttpClient
import java.io.InputStream

class OkHttpUrlLoader
    (private val client: Call.Factory) : ModelLoader<GlideUrl, InputStream> {
    override fun handles(url: GlideUrl): Boolean {
        return true
    }

    override fun buildLoadData(
        model: GlideUrl, width: Int, height: Int, options: Options
    ): LoadData<InputStream> {
        return LoadData(model, OkHttpStreamFetcher(client, model))
    }

    class Factory
    @JvmOverloads constructor(private val client: Call.Factory = internalClient!!) :
        ModelLoaderFactory<GlideUrl, InputStream> {
        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<GlideUrl, InputStream> {
            return OkHttpUrlLoader(client)
        }

        override fun teardown() {
            // Do nothing, this instance doesn't own the client.
        }

        companion object {
            @Volatile
            private var internalClient: Call.Factory? = null
                get() {
                    if (field == null) {
                        synchronized(Factory::class.java) {
                            if (field == null) {
                                val builder = OkHttpClient.Builder()
                                builder.addInterceptor(CustomInterceptor(false))
                                val client: OkHttpClient = builder.build()
                                client.dispatcher.maxRequests = 25
                                client.dispatcher.maxRequestsPerHost = 25
                                field = client
                            }
                        }
                    }
                    return field
                }
        }
    }
}
