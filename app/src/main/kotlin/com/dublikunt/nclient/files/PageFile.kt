package com.dublikunt.nclient.files

import android.content.Context
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import com.dublikunt.nclient.api.comments.Page
import com.dublikunt.nclient.enums.ImageExt
import com.dublikunt.nclient.settings.Global.findGalleryFolder
import java.io.File
import java.util.Objects
import java.util.regex.Pattern

open class PageFile : File, Parcelable {
    private val ext: ImageExt
    val page: Int

    constructor(ext: ImageExt, file: File, page: Int) : super(file.absolutePath) {
        this.ext = ext
        this.page = page
    }

    protected constructor(`in`: Parcel) : super(`in`.readString().toString()) {
        page = `in`.readInt()
        ext = ImageExt.values()[`in`.readByte().toInt()]
    }

    fun toUri(): Uri {
        return Uri.fromFile(this)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(this.absolutePath)
        dest.writeInt(page)
        dest.writeByte(ext.ordinal.toByte())
    }

    companion object CREATOR : Creator<PageFile> {
        override fun createFromParcel(parcel: Parcel): PageFile {
            return PageFile(parcel)
        }

        override fun newArray(size: Int): Array<PageFile?> {
            return arrayOfNulls(size)
        }

        private val DEFAULT_THUMBNAIL =
            Pattern.compile("^0*1\\.(gif|png|jpg)$", Pattern.CASE_INSENSITIVE)

        private fun fastThumbnail(folder: File): PageFile? {
            for (ext in ImageExt.values()) {
                val name = "001." + ext.name
                val file = File(folder, name)
                if (file.exists()) return PageFile(ext, file, 1)
            }
            return null
        }

        fun getThumbnail(context: Context, id: Int): PageFile? {
            val file = findGalleryFolder(context, id) ?: return null
            val pageFile = fastThumbnail(file)
            if (pageFile != null) return pageFile
            val files = file.listFiles() ?: return null
            for (f in files) {
                val m = DEFAULT_THUMBNAIL.matcher(f.name)
                if (!m.matches()) continue
                val ext = Page.charToExt(Objects.requireNonNull(m.group(1))[0].code)
                return PageFile(ext, f, 1)
            }
            return null
        }
    }
}
