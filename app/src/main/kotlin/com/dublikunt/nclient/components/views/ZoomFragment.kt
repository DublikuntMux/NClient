package com.dublikunt.nclient.components.views

import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.bumptech.glide.Priority
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.Rotate
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.ImageViewTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.dublikunt.nclient.R
import com.dublikunt.nclient.ZoomActivity
import com.dublikunt.nclient.api.gallerys.Gallery
import com.dublikunt.nclient.api.gallerys.GenericGallery
import com.dublikunt.nclient.components.GlideX.with
import com.dublikunt.nclient.components.photoview.PhotoView
import com.dublikunt.nclient.files.GalleryFolder
import com.dublikunt.nclient.files.PageFile
import com.dublikunt.nclient.settings.Global
import com.dublikunt.nclient.utility.LogUtility.download
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

class ZoomFragment : Fragment() {
    private lateinit var photoView: PhotoView
    private lateinit var retryButton: ImageButton
    private var pageFile: PageFile? = null
    private var url: Uri? = null
    private var degree = 0
    private var completedDownload = false
    private var clickListener: View.OnClickListener? = null
    private var zoomChangeListener: OnZoomChangeListener? = null
    private lateinit var target: ImageViewTarget<Drawable>

    fun setClickListener(clickListener: View.OnClickListener?) {
        this.clickListener = clickListener
    }

    fun setZoomChangeListener(zoomChangeListener: OnZoomChangeListener?) {
        this.zoomChangeListener = zoomChangeListener
    }

    private fun calculateScaleFactor(width: Int, height: Int): Float {
        val activity = activity
        if (height < width * 2) return Global.getDefaultZoom()
        var finalSize = Global.getDeviceWidth(activity as AppCompatActivity?).toFloat() * height /
            (Global.getDeviceHeight(activity).toFloat() * width)
        finalSize = max(finalSize, Global.getDefaultZoom())
        finalSize = min(finalSize, MAX_SCALE)
        download("Final scale: $finalSize")
        return floor(finalSize.toDouble()).toFloat()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_zoom, container, false)
        val activity = activity as ZoomActivity?
        assert(arguments != null)
        assert(activity != null)

        photoView = rootView.findViewById(R.id.image)
        retryButton = rootView.findViewById(R.id.imageView)

        val str = requireArguments().getString("URL")
        url = if (str == null) null else Uri.parse(str)
        pageFile = requireArguments().getParcelable("FOLDER")
        photoView.setAllowParentInterceptOnEdge(true)
        photoView.setOnPhotoTapListener { view, x, y ->
            val prev = x < CHANGE_PAGE_THRESHOLD
            val next = x > 1f - CHANGE_PAGE_THRESHOLD
            if ((prev || next) && Global.isButtonChangePage) {
                activity!!.changeClosePage(next)
            } else if (clickListener != null) {
                clickListener!!.onClick(view)
            }
            download(requireView(), x, y, prev, next)
        }
        photoView.setOnScaleChangeListener { _, _, _ ->
            if (zoomChangeListener != null) {
                zoomChangeListener!!.onZoomChange(rootView, photoView.scale)
            }
        }
        photoView.maximumScale = MAX_SCALE
        retryButton.setOnClickListener { loadImage() }
        createTarget()
        loadImage()
        return rootView
    }

    private fun createTarget() {
        target = object : ImageViewTarget<Drawable>(photoView) {
            override fun setResource(resource: Drawable?) {
                photoView.setImageDrawable(resource)
            }

            fun applyDrawable(toShow: ImageView?, toHide: ImageView?, drawable: Drawable?) {
                toShow!!.visibility = View.VISIBLE
                toHide!!.visibility = View.GONE
                toShow.setImageDrawable(drawable)
                if (toShow is PhotoView) scalePhoto(drawable)
            }

            override fun onLoadStarted(placeholder: Drawable?) {
                super.onLoadStarted(placeholder)
                applyDrawable(photoView, retryButton, placeholder)
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                super.onLoadFailed(errorDrawable)
                applyDrawable(retryButton, photoView, errorDrawable)
            }

            override fun onResourceReady(
                resource: Drawable,
                transition: Transition<in Drawable?>?
            ) {
                applyDrawable(photoView, retryButton, resource)
                if (resource is Animatable) (resource as GifDrawable).start()
            }

            override fun onLoadCleared(placeholder: Drawable?) {
                super.onLoadCleared(placeholder)
                applyDrawable(photoView, retryButton, placeholder)
            }
        }
    }

    private fun scalePhoto(drawable: Drawable?) {
        photoView.setScale(
            calculateScaleFactor(
                drawable!!.intrinsicWidth,
                drawable.intrinsicHeight
            ), 0f, 0f, false
        )
    }

    fun loadImage(priority: Priority? = Priority.NORMAL) {
        if (completedDownload) return
        cancelRequest()
        val dra = loadPage() ?: return
        dra
            .transform(Rotate(degree))
            .placeholder(R.mipmap.ic_launcher_foreground)
            .error(R.drawable.ic_refresh)
            .priority(priority!!)
            .addListener(object : RequestListener<Drawable?> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any,
                    target: Target<Drawable?>,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any,
                    target: Target<Drawable?>,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    completedDownload = true
                    return false
                }
            })
            .into(target)
    }

    private fun loadPage(): RequestBuilder<Drawable>? {
        val request: RequestBuilder<Drawable>
        val glide = with(photoView) ?: return null
        if (pageFile != null) {
            request = glide.load(pageFile)
            download("Requested file glide: $pageFile")
        } else {
            request = if (url == null) glide.load(R.mipmap.ic_launcher) else {
                download("Requested url glide: $url")
                glide.load(url)
            }
        }
        return request
    }

    val drawable: Drawable
        get() = photoView.drawable

    fun cancelRequest() {
        if (completedDownload) return
        val manager = with(photoView)
        manager?.clear(target)
    }

    private fun updateDegree() {
        degree = (degree + 270) % 360
        loadImage()
    }

    fun rotate() {
        updateDegree()
    }

    interface OnZoomChangeListener {
        fun onZoomChange(v: View?, zoomLevel: Float)
    }

    companion object {
        private const val MAX_SCALE = 4f
        private const val CHANGE_PAGE_THRESHOLD = .2f
        fun newInstance(
            gallery: GenericGallery,
            page: Int,
            directory: GalleryFolder?
        ): ZoomFragment {
            val args = Bundle()
            args.putString(
                "URL",
                if (gallery.isLocal) null else (gallery as Gallery).getPageUrl(page).toString()
            )
            args.putParcelable("FOLDER", directory?.getPage(page + 1))
            val fragment = ZoomFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
