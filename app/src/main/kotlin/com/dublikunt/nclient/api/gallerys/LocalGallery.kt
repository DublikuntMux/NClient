package com.dublikunt.nclient.api.gallerys

import android.graphics.BitmapFactory
import android.os.Parcel
import android.os.Parcelable.Creator
import android.util.JsonReader
import com.dublikunt.nclient.classes.Size
import com.dublikunt.nclient.enums.SpecialTagIds
import com.dublikunt.nclient.files.GalleryFolder
import com.dublikunt.nclient.utility.LogUtility.download
import java.io.File
import java.io.FileReader
import java.util.Objects
import java.util.regex.Pattern

class LocalGallery : GenericGallery {
    private val folder: GalleryFolder
    override val galleryData: GalleryData
    override val id: Int
        get() = folder.id
    override val type: Type = Type.LOCAL
    override val pageCount: Int
        get() = galleryData.pageCount
    override val valid: Boolean
    override val title: String
    val trueTitle: String
    private var hasAdvancedData = true
    override var maxSize = Size(0, 0)
    override var minSize = Size(Int.MAX_VALUE, Int.MAX_VALUE)
    override val galleryFolder: GalleryFolder?
        get() = null

    override fun hasGalleryData(): Boolean {
        return hasAdvancedData
    }

    private val DUP_PATTERN = Pattern.compile("^(.*)\\.DUP\\d+$")

    private fun createTitle(file: File): String {
        val name = file.name
        val matcher = DUP_PATTERN.matcher(name)
        if (!matcher.matches()) return name
        val title = matcher.group(1)
        return title ?: name
    }

    constructor(file: File, jumpDataRetrieve: Boolean = false) {
        val folder1: GalleryFolder? = try {
            GalleryFolder(file)
        } catch (ignore: IllegalArgumentException) {
            null
        }
        folder = folder1!!
        trueTitle = file.name
        title = createTitle(file)
        if (jumpDataRetrieve) {
            galleryData = GalleryData.fakeData()
        } else {
            galleryData = readGalleryData()
            if (galleryData.id == SpecialTagIds.INVALID_ID.toInt()) galleryData.id = id
        }
        galleryData.pageCount = folder.max
        valid = true && folder.pageCount > 0
    }

    private constructor(`in`: Parcel) {
        galleryData = Objects.requireNonNull(
            `in`.readParcelable(
                GalleryData::class.java.classLoader
            )
        )
        maxSize = Objects.requireNonNull(
            `in`.readParcelable(
                Size::class.java.classLoader
            )
        )
        minSize = Objects.requireNonNull(
            `in`.readParcelable(
                Size::class.java.classLoader
            )
        )
        trueTitle = `in`.readString().toString()
        title = `in`.readString()!!
        hasAdvancedData = `in`.readByte().toInt() == 1
        folder = `in`.readParcelable(GalleryFolder::class.java.classLoader)!!
        valid = true
    }

    private fun readGalleryData(): GalleryData {
        val nomedia = folder.galleryDataFile
        try {
            JsonReader(FileReader(nomedia)).use { reader -> return GalleryData(reader) }
        } catch (ignore: Exception) {
        }
        hasAdvancedData = false
        return GalleryData.fakeData()
    }

    fun calculateSizes() {
        for (f in folder) checkSize(f)
    }

    private fun checkSize(f: File) {
        download("Decoding: $f")
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(f.absolutePath, options)
        if (options.outWidth > maxSize.width) maxSize.width = options.outWidth
        if (options.outWidth < minSize.width) minSize.width = options.outWidth
        if (options.outHeight > maxSize.height) maxSize.height = options.outHeight
        if (options.outHeight < minSize.height) minSize.height = options.outHeight
    }

    val min: Int
        get() = folder.min
    val directory: File
        get() = folder.folder

    fun getPage(index: Int): File? {
        return folder.getPage(index)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(galleryData, flags)
        dest.writeParcelable(maxSize, flags)
        dest.writeParcelable(minSize, flags)
        dest.writeString(trueTitle)
        dest.writeString(title)
        dest.writeByte((if (hasAdvancedData) 1 else 0).toByte())
        dest.writeParcelable(folder, flags)
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val gallery = o as LocalGallery
        return folder == gallery.folder
    }

    override fun hashCode(): Int {
        return folder.hashCode()
    }

    override fun toString(): String {
        return "LocalGallery{" +
            "galleryData=" + galleryData +
            ", title='" + title + '\'' +
            ", folder=" + folder +
            ", valid=" + valid +
            ", maxSize=" + maxSize +
            ", minSize=" + minSize +
            '}'
    }

    companion object CREATOR : Creator<LocalGallery> {
        override fun createFromParcel(parcel: Parcel): LocalGallery {
            return LocalGallery(parcel)
        }

        override fun newArray(size: Int): Array<LocalGallery?> {
            return arrayOfNulls(size)
        }
    }
}
