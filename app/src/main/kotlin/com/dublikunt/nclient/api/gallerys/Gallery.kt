package com.dublikunt.nclient.api.gallerys

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable.Creator
import android.util.JsonReader
import android.util.JsonWriter
import com.dublikunt.nclient.api.comments.Page
import com.dublikunt.nclient.api.comments.Tag
import com.dublikunt.nclient.api.comments.TagList
import com.dublikunt.nclient.async.database.Queries
import com.dublikunt.nclient.async.database.Queries.TagTable.allOnlineBlacklisted
import com.dublikunt.nclient.async.database.Queries.TagTable.getAllStatus
import com.dublikunt.nclient.async.database.Queries.getColumnFromName
import com.dublikunt.nclient.classes.Size
import com.dublikunt.nclient.enums.*
import com.dublikunt.nclient.files.GalleryFolder
import com.dublikunt.nclient.settings.Global
import com.dublikunt.nclient.settings.Global.downloadPolicy
import com.dublikunt.nclient.settings.Global.removeAvoidedGalleries
import com.dublikunt.nclient.settings.Global.titleType
import com.dublikunt.nclient.utility.LogUtility.download
import com.dublikunt.nclient.utility.Utility
import org.jsoup.select.Elements
import java.io.IOException
import java.io.StringReader
import java.io.Writer
import java.util.*

class Gallery : GenericGallery {
    override val galleryData: GalleryData
    private val folder: GalleryFolder?
    val isOnlineFavorite: Boolean
    var related: MutableList<SimpleGallery> = ArrayList()
    var language = Language.UNKNOWN
        private set
    override val id: Int
        get() = galleryData.id
    override val type: Type
        get() = Type.COMPLETE
    override val pageCount: Int
        get() = galleryData.pageCount
    override val valid: Boolean
        get() = galleryData.isValid
    override val title: String
        get() {
            var x = getTitle(titleType)
            if (x.length > 2) return x
            if (getTitle(TitleType.PRETTY).also { x = it }.length > 2) return x
            if (getTitle(TitleType.ENGLISH).also { x = it }.length > 2) return x
            return if (getTitle(TitleType.JAPANESE).also { x = it }.length > 2) x else "Unnamed"
        }
    override var maxSize = Size(0, 0)
    override var minSize = Size(Int.MAX_VALUE, Int.MAX_VALUE)
    override val galleryFolder: GalleryFolder?
        get() = folder

    constructor(context: Context, json: String, related: Elements, isFavorite: Boolean) {
        download("Found JSON: $json")
        val reader = JsonReader(StringReader(json))
        this.related = ArrayList(related.size)
        for (e in related) this.related.add(SimpleGallery(context, e!!))
        galleryData = GalleryData(reader)
        folder = GalleryFolder.fromId(context, galleryData.id)
        calculateSizes(galleryData)
        language = loadLanguage(tags)
        isOnlineFavorite = isFavorite
    }

    constructor(cursor: Cursor, tags: TagList) {
        maxSize.width = cursor.getInt(getColumnFromName(cursor, Queries.GalleryTable.MAX_WIDTH))
        maxSize.height = cursor.getInt(getColumnFromName(cursor, Queries.GalleryTable.MAX_HEIGHT))
        minSize.width = cursor.getInt(getColumnFromName(cursor, Queries.GalleryTable.MIN_WIDTH))
        minSize.height = cursor.getInt(getColumnFromName(cursor, Queries.GalleryTable.MIN_HEIGHT))
        galleryData = GalleryData(cursor, tags)
        folder = GalleryFolder.fromId(null, galleryData.id)
        language = loadLanguage(tags)
        isOnlineFavorite = false
        download(toString())
    }

    constructor() {
        isOnlineFavorite = false
        galleryData = GalleryData.fakeData()
        folder = null
    }

    constructor(`in`: Parcel) {
        maxSize = `in`.readParcelable(Size::class.java.classLoader)!!
        minSize = `in`.readParcelable(Size::class.java.classLoader)!!
        galleryData = `in`.readParcelable(GalleryData::class.java.classLoader)!!
        folder = `in`.readParcelable(GalleryFolder::class.java.classLoader)
        `in`.readTypedList(related as List<SimpleGallery?>, SimpleGallery.CREATOR)
        isOnlineFavorite = `in`.readByte().toInt() == 1
        language = loadLanguage(tags)
    }

    private fun calculateSizes(galleryData: GalleryData) {
        var actualSize: Size
        for (page in galleryData.pages) {
            actualSize = page.size
            if (actualSize.width > maxSize.width) maxSize.width = actualSize.width
            if (actualSize.height > maxSize.height) maxSize.height = actualSize.height
            if (actualSize.width < minSize.width) minSize.width = actualSize.width
            if (actualSize.height < minSize.height) minSize.height = actualSize.height
        }
    }

    val pathTitle: String
        get() = getPathTitle(title)
    val cover: Uri
        get() {
            if (downloadPolicy === Global.DataUsageType.THUMBNAIL) return thumbnail
            return if (galleryData.cover.imageExt === ImageExt.GIF) getHighPage(0) else Uri.parse(
                String.format(
                    Locale.US,
                    "https://t." + Utility.host + "/galleries/%d/cover.%s",
                    mediaId,
                    Page.extToString(galleryData.thumbnail.imageExt)
                )
            )
        }
    val thumb: ImageExt
        get() = galleryData.thumbnail.imageExt
    val thumbnail: Uri
        get() = if (galleryData.cover.imageExt === ImageExt.GIF) getHighPage(0) else Uri.parse(
            String.format(
                Locale.US,
                "https://t." + Utility.host + "/galleries/%d/thumb.%s",
                mediaId,
                Page.extToString(galleryData.thumbnail.imageExt)
            )
        )

    private fun getFileUri(page: Int): Uri? {
        if (folder == null) return null
        val f = folder.getPage(page + 1) ?: return null
        return f.toUri()
    }

    fun getPageUrl(page: Int): Uri {
        if (downloadPolicy === Global.DataUsageType.THUMBNAIL) return getLowPage(page)
        val uri = getFileUri(page)
        return uri ?: getHighPage(page)
    }

    fun getHighPage(page: Int): Uri {
        return Uri.parse(
            String.format(
                Locale.US,
                "https://i." + Utility.host + "/galleries/%d/%d.%s",
                mediaId,
                page + 1,
                getPageExtension(page)
            )
        )
    }

    fun getLowPage(page: Int): Uri {
        val uri = getFileUri(page)
        return uri
            ?: Uri.parse(
                String.format(
                    Locale.US,
                    "https://t." + Utility.host + "/galleries/%d/%dt.%s",
                    mediaId,
                    page + 1,
                    getPageExtension(page)
                )
            )
    }

    fun getPageExtension(page: Int): String {
        return Page.extToString(getPage(page).imageExt)
    }

    private fun getPage(index: Int): Page {
        return galleryData.getPage(index)
    }

    fun toSimpleGallery(): SimpleGallery {
        return SimpleGallery(this)
    }

    val isRelatedLoaded: Boolean
        get() = true

    private fun getTitle(x: TitleType): String {
        return galleryData.getTitle(x)
    }

    private val uploadDate: Date
        get() = galleryData.uploadDate
    private val favoriteCount: Int
        get() = galleryData.favoriteCount

    val tags: TagList
        get() = galleryData.tags

    val mediaId: Int
        get() = galleryData.mediaId

    private fun hasIgnoredTags(s: Set<Tag>): Boolean {
        for (t in tags.allTagsSet) if (s.contains(t)) {
            download("Found: " + s + ",," + t.toQueryTag())
            return true
        }
        return false
    }

    fun hasIgnoredTags(): Boolean {
        val tags: MutableSet<Tag> = HashSet(getAllStatus(TagStatus.AVOIDED))
        if (removeAvoidedGalleries()) tags.addAll(allOnlineBlacklisted)
        return hasIgnoredTags(tags)
    }

    override fun hasGalleryData(): Boolean {
        return true
    }

    @Throws(IOException::class)
    fun jsonWrite(ww: Writer) {
        val writer = JsonWriter(ww)
        writer.beginObject()
        writer.name("id").value(id.toLong())
        writer.name("media_id").value(mediaId.toLong())
        writer.name("upload_date").value(uploadDate.time / 1000)
        writer.name("num_favorites").value(favoriteCount.toLong())
        toJsonTitle(writer)
        toJsonTags(writer)
        writer.endObject()
        writer.flush()
    }

    @Throws(IOException::class)
    private fun toJsonTags(writer: JsonWriter) {
        writer.name("tags")
        writer.beginArray()
        for (t in tags.allTagsSet) t.writeJson(writer)
        writer.endArray()
    }

    @Throws(IOException::class)
    private fun toJsonTitle(writer: JsonWriter) {
        var title: String?
        writer.name("title")
        writer.beginObject()
        if (getTitle(TitleType.JAPANESE).also { title = it } != null) writer.name("japanese")
            .value(title)
        if (getTitle(TitleType.PRETTY).also { title = it } != null) writer.name("pretty")
            .value(title)
        if (getTitle(TitleType.ENGLISH).also { title = it } != null) writer.name("english")
            .value(title)
        writer.endObject()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(maxSize, flags)
        dest.writeParcelable(minSize, flags)
        dest.writeParcelable(galleryData, flags)
        dest.writeParcelable(folder, flags)
        dest.writeTypedList(related)
        dest.writeByte((if (isOnlineFavorite) 1 else 0).toByte())
    }

    override fun toString(): String {
        return "Gallery{" +
            "galleryData=" + galleryData +
            ", language=" + language +
            ", maxSize=" + maxSize +
            ", minSize=" + minSize +
            ", onlineFavorite=" + isOnlineFavorite +
            '}'
    }

    companion object CREATOR : Creator<Gallery> {
        override fun createFromParcel(parcel: Parcel): Gallery {
            return Gallery(parcel)
        }

        override fun newArray(size: Int): Array<Gallery?> {
            return arrayOfNulls(size)
        }

        fun getPathTitle(title: String?, defaultValue: String): String {
            if (title == null) return defaultValue
            var pathTitle = title.replace('/', ' ').replace("[/|\\\\*\"'?:<>]".toRegex(), " ")
            while (pathTitle.contains("  ")) pathTitle = pathTitle.replace("  ", " ")
            return pathTitle.trim { it <= ' ' }
        }

        fun getPathTitle(title: String?): String {
            return getPathTitle(title, "")
        }

        fun loadLanguage(tags: TagList): Language {
            for (tag in tags.retrieveForType(TagType.LANGUAGE)!!) {
                when (tag.id.toShort()) {
                    SpecialTagIds.LANGUAGE_JAPANESE -> return Language.JAPANESE
                    SpecialTagIds.LANGUAGE_ENGLISH -> return Language.ENGLISH
                    SpecialTagIds.LANGUAGE_CHINESE -> return Language.CHINESE
                }
            }
            return Language.UNKNOWN
        }

        fun emptyGallery(): Gallery {
            return Gallery()
        }
    }
}
