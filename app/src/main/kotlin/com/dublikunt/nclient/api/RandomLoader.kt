package com.dublikunt.nclient.api

import com.dublikunt.nclient.RandomActivity
import com.dublikunt.nclient.api.Inspector.DefaultInspectorResponse
import com.dublikunt.nclient.api.Inspector.InspectorResponse
import com.dublikunt.nclient.api.components.Gallery
import com.dublikunt.nclient.api.components.GenericGallery
import com.dublikunt.nclient.utility.ImageDownloadUtility.preloadImage
import kotlin.properties.Delegates

class RandomLoader(private val activity: RandomActivity) {
    private var galleries: MutableList<Gallery> = ArrayList(MAXLOADED)
    private var galleryHasBeenRequested by Delegates.notNull<Boolean>()
    private fun loadRandomGallery() {
        if (galleries.size >= MAXLOADED) return
        Inspector.randomInspector(activity, response, false).start()
    }

    fun requestGallery() {
        galleryHasBeenRequested = true
        var i = 0
        while (i < galleries.size) {
            i++
        }
        if (galleries.size > 0) {
            val gallery = galleries.removeAt(0)
            activity.runOnUiThread { activity.loadGallery(gallery) }
            galleryHasBeenRequested = false
        }
        loadRandomGallery()
    }

    private val response: InspectorResponse = object : DefaultInspectorResponse() {
        override fun onFailure(e: Exception) {
            loadRandomGallery()
        }

        override fun onSuccess(galleryList: List<GenericGallery>) {
            if (galleryList.isEmpty() || !galleryList[0].valid) {
                loadRandomGallery()
                return
            }
            val gallery = galleryList[0] as Gallery
            galleries.add(gallery)
            preloadImage(activity, gallery.cover)
            if (galleryHasBeenRequested) requestGallery() else if (galleries.size < MAXLOADED) loadRandomGallery()
        }
    }

    init {
        galleryHasBeenRequested = RandomActivity.loadedGallery == null
        loadRandomGallery()
    }

    companion object {
        private const val MAXLOADED = 5
    }
}
