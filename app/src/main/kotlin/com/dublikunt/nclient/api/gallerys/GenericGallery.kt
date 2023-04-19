package com.dublikunt.nclient.api.gallerys

import android.os.Parcelable
import com.dublikunt.nclient.classes.Size
import com.dublikunt.nclient.files.GalleryFolder
import com.dublikunt.nclient.utility.Utility
import java.util.Locale

abstract class GenericGallery : Parcelable {
    abstract val id: Int
    abstract val type: Type
    abstract val pageCount: Int
    abstract val valid: Boolean
    abstract val title: String
    val isLocal: Boolean
        get() = type == Type.LOCAL

    abstract val maxSize: Size?
    abstract val minSize: Size?
    abstract val galleryFolder: GalleryFolder?
    abstract val galleryData: GalleryData?

    abstract fun hasGalleryData(): Boolean

    enum class Type {
        COMPLETE, LOCAL, SIMPLE
    }

    fun sharePageUrl(i: Int): String {
        return String.format(Locale.US, "https://" + Utility.host + "/g/%d/%d/", id, i + 1)
    }
}