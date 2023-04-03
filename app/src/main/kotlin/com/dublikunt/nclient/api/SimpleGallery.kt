package com.dublikunt.nclient.api

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable.Creator
import com.dublikunt.nclient.api.components.*
import com.dublikunt.nclient.async.database.Queries
import com.dublikunt.nclient.async.database.Queries.TagTable.getTagsFromListOfInt
import com.dublikunt.nclient.classes.Size
import com.dublikunt.nclient.enums.ImageExt
import com.dublikunt.nclient.enums.Language
import com.dublikunt.nclient.enums.TagStatus
import com.dublikunt.nclient.files.GalleryFolder
import com.dublikunt.nclient.settings.Global.maxId
import com.dublikunt.nclient.settings.Global.updateMaxId
import com.dublikunt.nclient.utility.LogUtility.download
import com.dublikunt.nclient.utility.Utility
import org.jsoup.nodes.Element
import java.util.*

class SimpleGallery : GenericGallery {
    override val title: String
    override val maxSize: Size?
        get() = null
    override val minSize: Size?
        get() = null
    override val galleryFolder: GalleryFolder?
        get() = null
    override val galleryData: GalleryData?
        get() = null
    val thumb: ImageExt
    override val id: Int
    override val type = Type.SIMPLE
    override val pageCount = 0
    override val valid: Boolean
        get() = id > 0
    val mediaId: Int
    var language = Language.UNKNOWN
        private set
    private var tags: TagList? = null

    constructor(`in`: Parcel) {
        title = `in`.readString()!!
        id = `in`.readInt()
        mediaId = `in`.readInt()
        thumb = ImageExt.values()[`in`.readByte().toInt()]
        language = Language.values()[`in`.readByte().toInt()]
    }

    @SuppressLint("Range")
    constructor(c: Cursor) {
        title = c.getString(c.getColumnIndex(Queries.HistoryTable.TITLE))
        id = c.getInt(c.getColumnIndex(Queries.HistoryTable.ID))
        mediaId = c.getInt(c.getColumnIndex(Queries.HistoryTable.MEDIAID))
        thumb = ImageExt.values()[c.getInt(c.getColumnIndex(Queries.HistoryTable.THUMB))]
    }

    constructor(context: Context?, e: Element) {
        var temp: String
        val tags = e.attr("data-tags").replace(' ', ',')
        this.tags = getTagsFromListOfInt(tags)
        language = Gallery.loadLanguage(this.tags!!)
        var a = e.getElementsByTag("a").first()!!
        temp = a.attr("href")
        id = temp.substring(3, temp.length - 1).toInt()
        a = e.getElementsByTag("img").first()!!
        temp = if (a.hasAttr("data-src")) a.attr("data-src") else a.attr("src")
        mediaId = temp.substring(temp.indexOf("galleries") + 10, temp.lastIndexOf('/')).toInt()
        thumb = Page.charToExt(temp[temp.length - 3].code)
        title = e.getElementsByTag("div").first()!!.text()
        if (context != null && id > maxId) updateMaxId(context, id)
    }

    constructor(gallery: Gallery) {
        title = gallery.title
        mediaId = gallery.mediaId
        id = gallery.id
        thumb = gallery.thumb
    }

    fun hasIgnoredTags(s: String): Boolean {
        if (tags == null) return false
        for (t in tags!!.allTagsList) if (s.contains(t.toQueryTag(TagStatus.AVOIDED))) {
            download("Found: " + s + ",," + t.toQueryTag())
            return true
        }
        return false
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(title)
        dest.writeInt(id)
        dest.writeInt(mediaId)
        dest.writeByte(thumb.ordinal.toByte())
        dest.writeByte(language.ordinal.toByte())
    }

    fun getThumbnail(): Uri {
        return if (thumb === ImageExt.GIF) {
            Uri.parse(
                String.format(
                    Locale.US,
                    "https://i." + Utility.host + "/galleries/%d/1.gif",
                    mediaId
                )
            )
        } else Uri.parse(
            String.format(
                Locale.US,
                "https://t." + Utility.host + "/galleries/%d/thumb.%s",
                mediaId,
                extToString(
                    thumb
                )
            )
        )
    }

    override fun toString(): String {
        return "SimpleGallery{" +
            "language=" + language +
            ", title='" + title + '\'' +
            ", thumbnail=" + thumb +
            ", id=" + id +
            ", mediaId=" + mediaId +
            '}'
    }

    override fun hasGalleryData(): Boolean {
        return false
    }

    companion object {
        @JvmField
        val CREATOR: Creator<SimpleGallery?> = object : Creator<SimpleGallery?> {
            override fun createFromParcel(`in`: Parcel): SimpleGallery {
                return SimpleGallery(`in`)
            }

            override fun newArray(size: Int): Array<SimpleGallery?> {
                return arrayOfNulls(size)
            }
        }

        private fun extToString(ext: ImageExt): String {
            return when (ext) {
                ImageExt.GIF -> "gif"
                ImageExt.PNG -> "png"
                ImageExt.JPG -> "jpg"
            }
        }
    }
}
