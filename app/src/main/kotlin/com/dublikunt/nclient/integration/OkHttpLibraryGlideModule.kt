package com.dublikunt.nclient.integration

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import com.github.piasy.biv.BigImageViewer
import com.github.piasy.biv.loader.glide.GlideImageLoader
import java.io.InputStream


@GlideModule
class OkHttpLibraryGlideModule : AppGlideModule() {
    override fun registerComponents(
        context: Context, glide: Glide, registry: Registry
    ) {
        registry.replace(
            GlideUrl::class.java,
            InputStream::class.java, OkHttpUrlLoader.Factory()
        )
        BigImageViewer.initialize(GlideImageLoader.with(context))
    }

    override fun isManifestParsingEnabled(): Boolean {
        return false
    }

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        val calculator = MemorySizeCalculator.Builder(context)
            .setMemoryCacheScreens(2f)
            .build()
        builder.setMemoryCache(LruResourceCache(calculator.memoryCacheSize.toLong()))
    }
}
