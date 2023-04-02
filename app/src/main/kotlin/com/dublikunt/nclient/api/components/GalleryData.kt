package com.dublikunt.nclient.api.components

import android.database.Cursor
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import android.util.JsonReader
import android.util.JsonToken
import com.dublikunt.nclient.async.database.Queries
import com.dublikunt.nclient.async.database.Queries.TagTable.insert
import com.dublikunt.nclient.async.database.Queries.getColumnFromName
import com.dublikunt.nclient.enums.ImageExt
import com.dublikunt.nclient.enums.ImageType
import com.dublikunt.nclient.enums.SpecialTagIds
import com.dublikunt.nclient.enums.TitleType
import com.dublikunt.nclient.utility.Utility
import java.io.IOException
import java.io.StringReader
import java.io.StringWriter
import java.util.*

open class GalleryData : Parcelable {
    var uploadDate = Date(0)
        private set
    var favoriteCount = 0
        private set
    var id = 0
    var pageCount = 0
    var mediaId = 0
        private set
    private var titles = arrayOf("", "", "")
    var tags = TagList()
        private set
    var cover = Page()
        private set
    var thumbnail = Page()
        private set
    var pages = ArrayList<Page>()
        private set
    var isValid = true
        private set

    private constructor()
    constructor(jr: JsonReader) {
        parseJSON(jr)
    }

    constructor(cursor: Cursor, tagList: TagList) {
        id = cursor.getInt(getColumnFromName(cursor, Queries.GalleryTable.IDGALLERY))
        mediaId = cursor.getInt(getColumnFromName(cursor, Queries.GalleryTable.MEDIAID))
        favoriteCount =
            cursor.getInt(getColumnFromName(cursor, Queries.GalleryTable.FAVORITE_COUNT))
        titles[TitleType.JAPANESE.ordinal] =
            cursor.getString(getColumnFromName(cursor, Queries.GalleryTable.TITLE_JP))
        titles[TitleType.PRETTY.ordinal] =
            cursor.getString(getColumnFromName(cursor, Queries.GalleryTable.TITLE_PRETTY))
        titles[TitleType.ENGLISH.ordinal] =
            cursor.getString(getColumnFromName(cursor, Queries.GalleryTable.TITLE_ENG))
        uploadDate = Date(cursor.getLong(getColumnFromName(cursor, Queries.GalleryTable.UPLOAD)))
        readPagePath(cursor.getString(getColumnFromName(cursor, Queries.GalleryTable.PAGES)))
        pageCount = pages.size
        tags = tagList
    }

    protected constructor(`in`: Parcel) {
        uploadDate = Date(`in`.readLong())
        favoriteCount = `in`.readInt()
        id = `in`.readInt()
        pageCount = `in`.readInt()
        mediaId = `in`.readInt()
        titles = Objects.requireNonNull(`in`.createStringArray())
        tags = Objects.requireNonNull(`in`.readParcelable(TagList::class.java.classLoader))
        cover = Objects.requireNonNull(
            `in`.readParcelable(
                Page::class.java.classLoader
            )
        )
        thumbnail = Objects.requireNonNull(
            `in`.readParcelable(
                Page::class.java.classLoader
            )
        )
        pages = Objects.requireNonNull(`in`.createTypedArrayList(Page.CREATOR)) as ArrayList<Page>
        isValid = `in`.readByte().toInt() != 0
    }

    @Throws(IOException::class)
    private fun parseJSON(jr: JsonReader) {
        jr.beginObject()
        while (jr.peek() != JsonToken.END_OBJECT) {
            when (jr.nextName()) {
                "upload_date" -> uploadDate = Date(jr.nextLong() * 1000)
                "num_favorites" -> favoriteCount = jr.nextInt()
                "num_pages" -> pageCount = jr.nextInt()
                "media_id" -> mediaId = jr.nextInt()
                "id" -> id = jr.nextInt()
                "images" -> readImages(jr)
                "title" -> readTitles(jr)
                "tags" -> readTags(jr)
                "error" -> {
                    jr.skipValue()
                    isValid = false
                }
                else -> jr.skipValue()
            }
        }
        jr.endObject()
    }

    private fun setTitle(type: TitleType, title: String) {
        titles[type.ordinal] = Utility.unescapeUnicodeString(title)
    }

    @Throws(IOException::class)
    private fun readTitles(jr: JsonReader) {
        jr.beginObject()
        while (jr.peek() != JsonToken.END_OBJECT) {
            when (jr.nextName()) {
                "japanese" -> setTitle(
                    TitleType.JAPANESE,
                    if (jr.peek() != JsonToken.NULL) jr.nextString() else ""
                )
                "english" -> setTitle(
                    TitleType.ENGLISH,
                    if (jr.peek() != JsonToken.NULL) jr.nextString() else ""
                )
                "pretty" -> setTitle(
                    TitleType.PRETTY,
                    if (jr.peek() != JsonToken.NULL) jr.nextString() else ""
                )
                else -> jr.skipValue()
            }
            if (jr.peek() == JsonToken.NULL) jr.skipValue()
        }
        jr.endObject()
    }

    @Throws(IOException::class)
    private fun readTags(jr: JsonReader) {
        jr.beginArray()
        while (jr.hasNext()) {
            val createdTag = Tag(jr)
            insert(createdTag)
            tags.addTag(createdTag)
        }
        jr.endArray()
        tags.sort { o1: Tag, o2: Tag -> o2.count - o1.count }
    }

    @Throws(IOException::class)
    private fun readImages(jr: JsonReader) {
        var actualPage = 0
        jr.beginObject()
        while (jr.peek() != JsonToken.END_OBJECT) {
            when (jr.nextName()) {
                "cover" -> cover = Page(ImageType.COVER, jr)
                "thumbnail" -> thumbnail = Page(ImageType.THUMBNAIL, jr)
                "pages" -> {
                    jr.beginArray()
                    while (jr.hasNext()) pages.add(Page(ImageType.PAGE, jr, actualPage++))
                    jr.endArray()
                }
                else -> jr.skipValue()
            }
        }
        jr.endObject()
        pages.trimToSize()
    }

    fun getTitle(i: Int): String {
        return titles[i]
    }

    fun getTitle(type: TitleType): String {
        return titles[type.ordinal]
    }

    fun getPage(index: Int): Page {
        return pages[index]
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(uploadDate.time)
        dest.writeInt(favoriteCount)
        dest.writeInt(id)
        dest.writeInt(pageCount)
        dest.writeInt(mediaId)
        dest.writeStringArray(titles)
        dest.writeParcelable(tags, flags)
        dest.writeParcelable(cover, flags)
        dest.writeParcelable(thumbnail, flags)
        dest.writeTypedList(pages)
        dest.writeByte((if (isValid) 1 else 0).toByte())
    }

    private fun writeInterval(writer: StringWriter, intervalLen: Int, referencePage: ImageExt) {
        writer.write(intervalLen.toString())
        writer.write(Page.extToChar(referencePage).code)
    }

    fun createPagePath(): String {
        val writer = StringWriter()
        writer.write(pages.size.toString())
        writer.write(cover.imageExtChar.code)
        writer.write(thumbnail.imageExtChar.code)
        if (pages.size == 0) return writer.toString()
        var referencePage = pages[0].imageExt
        var actualPage: ImageExt
        var intervalLen = 1
        for (i in 1 until pages.size) {
            actualPage = pages[i].imageExt
            if (actualPage !== referencePage) {
                writeInterval(writer, intervalLen, referencePage)
                referencePage = actualPage
                intervalLen = 1
            } else intervalLen++
        }
        writeInterval(writer, intervalLen, referencePage)
        return writer.toString()
    }

    @Throws(IOException::class)
    private fun readPagePath(path: String) {
        println(path)
        val reader = StringReader(path + "e")
        var absolutePage = 0
        var actualChar: Int
        var pageOfType = 0
        var specialImages = true
        while (reader.read().also { actualChar = it } != 'e'.code) {
            when (actualChar) {
                'p'.code, 'j'.code, 'g'.code -> {
                    if (specialImages) {
                        cover = Page(ImageType.COVER, Page.charToExt(actualChar))
                        thumbnail = Page(ImageType.THUMBNAIL, Page.charToExt(actualChar))
                        specialImages = false
                    } else {
                        var j = 0
                        while (j < pageOfType) {
                            pages.add(
                                Page(
                                    ImageType.PAGE,
                                    Page.charToExt(actualChar),
                                    absolutePage++
                                )
                            )
                            j++
                        }
                    }
                    pageOfType = 0
                }
                '0'.code, '1'.code, '2'.code, '3'.code, '4'.code, '5'.code, '6'.code, '7'.code, '8'.code, '9'.code -> {
                    pageOfType *= 10
                    pageOfType += actualChar - '0'.code
                }
                else -> {}
            }
        }
    }

    override fun toString(): String {
        return "GalleryData{" +
            "uploadDate=" + uploadDate +
            ", favoriteCount=" + favoriteCount +
            ", id=" + id +
            ", pageCount=" + pageCount +
            ", mediaId=" + mediaId +
            ", titles=" + titles.contentToString() +
            ", tags=" + tags +
            ", cover=" + cover +
            ", thumbnail=" + thumbnail +
            ", pages=" + pages +
            ", valid=" + isValid +
            '}'
    }

    companion object {
        @JvmField
        val CREATOR: Creator<GalleryData?> = object : Creator<GalleryData?> {
            override fun createFromParcel(`in`: Parcel): GalleryData {
                return GalleryData(`in`)
            }

            override fun newArray(size: Int): Array<GalleryData?> {
                return arrayOfNulls(size)
            }
        }

        fun fakeData(): GalleryData {
            val galleryData = GalleryData()
            galleryData.id = SpecialTagIds.INVALID_ID.toInt()
            galleryData.favoriteCount = -1
            galleryData.pageCount = -1
            galleryData.mediaId = SpecialTagIds.INVALID_ID.toInt()
            galleryData.pages.trimToSize()
            galleryData.isValid = false
            return galleryData
        }
    }
}
