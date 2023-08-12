package com.dublikunt.nclient.api

import com.dublikunt.nclient.RandomActivity
import com.dublikunt.nclient.api.Inspector.DefaultInspectorResponse
import com.dublikunt.nclient.api.Inspector.InspectorResponse
import com.dublikunt.nclient.api.components.Gallery
import com.dublikunt.nclient.api.components.GenericGallery
import com.dublikunt.nclient.utility.ImageDownloadUtility

class RandomLoader(private val activity: RandomActivity) {
    private val maxLoaded = 5

    private val galleries: MutableList<Gallery?> = ArrayList(maxLoaded)
    private var galleryHasBeenRequested: Boolean = RandomActivity.loadedGallery == null

    init {
        loadRandomGallery()
    }

    private fun loadRandomGallery() {
        if (galleries.size >= maxLoaded) return
        Inspector.randomInspector(activity, response, false).start()
    }

    fun requestGallery() {
        galleryHasBeenRequested = true
        var i = 0
        while (i < galleries.size) {
            if (galleries[i] == null) galleries.removeAt(i--)
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
            if (galleryList.isEmpty() || !galleryList[0].isValid) {
                loadRandomGallery()
                return
            }
            val gallery = galleryList[0] as Gallery
            galleries.add(gallery)
            ImageDownloadUtility.preloadImage(activity, gallery.cover)
            if (galleryHasBeenRequested) requestGallery() else if (galleries.size < maxLoaded) loadRandomGallery()
        }
    }
}
