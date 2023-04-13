package com.dublikunt.nclient.utility

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.load.resource.bitmap.Rotate
import com.bumptech.glide.request.target.ImageViewTarget
import com.dublikunt.nclient.api.components.Gallery
import com.dublikunt.nclient.components.GlideX
import com.dublikunt.nclient.settings.Global
import java.io.File

object ImageDownloadUtility {
    @JvmStatic
    fun preloadImage(context: Context, url: Uri) {
        if (Global.downloadPolicy == Global.DataUsageType.NONE) return
        val manager = GlideX.with(context)
        LogUtility.download("Requested url glide: $url")
        manager?.load(url)?.preload()
    }


    fun loadImageOp(context: Context, view: ImageView, file: File, angle: Int) {
        val glide = GlideX.with(context) ?: return
        val logo = Global.getLogo(context.resources)
        glide.load(file).transform(Rotate(angle)).error(logo).placeholder(logo).into(view)
        LogUtility.download("Requested file glide: $file")
    }


    fun loadImageOp(context: Context, view: ImageView, gallery: Gallery, page: Int, angle: Int) {
        val url = getUrlForGallery(gallery, page, true)
        loadImageOp(context, view, url, angle)
    }


    fun loadImageOp(context: Context, view: ImageView, url: Uri?, angle: Int) {
        LogUtility.download("Requested url glide: $url")
        if (Global.downloadPolicy == Global.DataUsageType.NONE) {
            loadLogo(view)
            return
        }
        val glide = GlideX.with(context) ?: return
        val logo = Global.getLogo(context.resources)
        var dra = glide.load(url)
        if (angle != 0) dra = dra.transform(Rotate(angle))
        dra.error(logo)
            .placeholder(logo)
            .into(object : ImageViewTarget<Drawable>(view) {
                override fun setResource(resource: Drawable?) {
                    this.view.setImageDrawable(resource)
                }
            })
    }

    private fun getUrlForGallery(gallery: Gallery, page: Int, shouldFull: Boolean): Uri {
        return if (shouldFull) gallery.getPageUrl(page) else gallery.getLowPage(page)
    }


    fun downloadPage(
        activity: AppCompatActivity,
        imageView: ImageView,
        gallery: Gallery,
        page: Int,
        shouldFull: Boolean
    ) {
        var shouldFull = shouldFull
        shouldFull = gallery.getPageExtension(page) == "gif" || shouldFull
        loadImageOp(activity, imageView, getUrlForGallery(gallery, page, shouldFull), 0)
    }


    private fun loadLogo(imageView: ImageView) {
        imageView.setImageDrawable(Global.getLogo(imageView.resources))
    }

    @JvmStatic
    fun loadImage(activity: AppCompatActivity, url: Uri?, imageView: ImageView) {
        loadImageOp(activity, imageView, url, 0)
    }

    @JvmStatic
    fun loadImage(activity: AppCompatActivity, file: File?, imageView: ImageView) {
        loadImage(activity, if (file == null) null else Uri.fromFile(file), imageView)
    }

    @JvmStatic
    fun loadImage(@DrawableRes resource: Int, imageView: ImageView) {
        imageView.setImageResource(resource)
    }
}
