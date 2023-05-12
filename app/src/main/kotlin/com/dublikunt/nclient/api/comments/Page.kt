package com.dublikunt.nclient.api.comments

import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import android.util.JsonReader
import android.util.JsonToken
import com.dublikunt.nclient.classes.Size
import com.dublikunt.nclient.enums.ImageExt
import com.dublikunt.nclient.enums.ImageType

open class Page : Parcelable {
    val page: Int
    private val imageType: ImageType
    lateinit var imageExt: ImageExt
    var size: Size = Size(0, 0)

    internal constructor() {
        imageType = ImageType.PAGE
        imageExt = ImageExt.JPG
        page = 0
    }

    constructor(type: ImageType, ext: ImageExt, page: Int = 0) {
        imageType = type
        imageExt = ext
        this.page = page
    }

    constructor(type: ImageType, reader: JsonReader, page: Int = 0) {
        imageType = type
        this.page = page
        reader.beginObject()
        while (reader.peek() != JsonToken.END_OBJECT) {
            when (reader.nextName()) {
                "t" -> imageExt = stringToExt(reader.nextString())
                "w" -> size.width = reader.nextInt()
                "h" -> size.height = reader.nextInt()
                else -> reader.skipValue()
            }
        }
        reader.endObject()
    }

    protected constructor(`in`: Parcel) {
        page = `in`.readInt()
        size = `in`.readParcelable(Size::class.java.classLoader)!!
        imageExt = ImageExt.values()[`in`.readByte().toInt()]
        imageType = ImageType.values()[`in`.readByte().toInt()]
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(page)
        dest.writeParcelable(size, flags)
        dest.writeByte((imageExt.ordinal).toByte())
        dest.writeByte(imageType.ordinal.toByte())
    }

    val imageExtChar: Char
        get() = extToChar(imageExt)

    override fun toString(): String {
        return "Page{" +
            "page=" + page +
            ", imageExt=" + imageExt +
            ", imageType=" + imageType +
            ", size=" + size +
            '}'
    }

    companion object CREATOR : Creator<Page> {
        override fun createFromParcel(parcel: Parcel): Page {
            return Page(parcel)
        }

        override fun newArray(size: Int): Array<Page?> {
            return arrayOfNulls(size)
        }

        fun stringToExt(ext: String): ImageExt {
            return charToExt(ext[0].code)
        }

        fun extToString(ext: ImageExt): String {
            return when (ext) {
                ImageExt.GIF -> "gif"
                ImageExt.PNG -> "png"
                ImageExt.JPG -> "jpg"
            }
        }

        fun extToChar(imageExt: ImageExt): Char {
            return when (imageExt) {
                ImageExt.GIF -> 'g'
                ImageExt.PNG -> 'p'
                ImageExt.JPG -> 'j'
            }
        }

        fun charToExt(ext: Int): ImageExt {
            return when (ext) {
                'g'.code -> ImageExt.GIF
                'p'.code -> ImageExt.PNG
                'j'.code -> ImageExt.JPG
                else -> ImageExt.JPG
            }
        }
    }
}
