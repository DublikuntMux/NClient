package com.dublikunt.nclient.api.comments

import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import android.util.JsonReader
import android.util.JsonToken
import android.util.JsonWriter
import com.dublikunt.nclient.enums.TagStatus
import com.dublikunt.nclient.enums.TagType
import com.dublikunt.nclient.enums.TagType.CREATOR.typeByName
import com.dublikunt.nclient.utility.LogUtility.download
import java.io.IOException
import java.util.*

class Tag : Parcelable {
    lateinit var name: String
        private set
    var count = 0
        private set
    var id = 0
        private set
    lateinit var type: TagType
        private set
    var status = TagStatus.DEFAULT

    constructor(text: String) {
        var text = text
        count = text.substring(0, text.indexOf(',')).toInt()
        text = text.substring(text.indexOf(',') + 1)
        id = text.substring(0, text.indexOf(',')).toInt()
        text = text.substring(text.indexOf(',') + 1)
        type = TagType.values[text.substring(0, text.indexOf(',')).toInt()]
        name = text.substring(text.indexOf(',') + 1)
    }

    constructor(name: String, count: Int, id: Int, type: TagType, status: TagStatus) {
        this.name = name
        this.count = count
        this.id = id
        this.type = type
        this.status = status
    }

    constructor(jr: JsonReader) {
        jr.beginObject()
        while (jr.peek() != JsonToken.END_OBJECT) {
            when (jr.nextName()) {
                "count" -> count = jr.nextInt()
                "type" -> type = typeByName(jr.nextString())
                "id" -> id = jr.nextInt()
                "name" -> name = jr.nextString()
                "url" -> download("Tag URL: " + jr.nextString())
                else -> jr.skipValue()
            }
        }
        jr.endObject()
    }

    private constructor(`in`: Parcel) {
        name = `in`.readString().toString()
        count = `in`.readInt()
        id = `in`.readInt()
        type = `in`.readParcelable(TagType::class.java.classLoader)!!
        status = TagStatus.values()[`in`.readByte().toInt()]
    }

    fun toQueryTag(status: TagStatus = this.status): String {
        val builder = StringBuilder()
        if (status === TagStatus.AVOIDED) builder.append('-')
        builder
            .append(type.single)
            .append(':')
            .append('"')
            .append(name)
            .append('"')
        return builder.toString()
    }

    @Throws(IOException::class)
    fun writeJson(writer: JsonWriter) {
        writer.beginObject()
        writer.name("count").value(count.toLong())
        writer.name("type").value(this.type.single)
        writer.name("id").value(id.toLong())
        writer.name("name").value(name)
        writer.endObject()
    }

    override fun toString(): String {
        return "Tag{" +
            "name='" + name + '\'' +
            ", count=" + count +
            ", id=" + id +
            ", type=" + type +
            ", status=" + status +
            '}'
    }

    fun toScrapedString(): String {
        return String.format(Locale.US, "%d,%d,%d,%s", count, id, type.id, name)
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val tag = o as Tag
        return id == tag.id
    }

    override fun hashCode(): Int {
        return id
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeInt(count)
        parcel.writeInt(id)
        parcel.writeParcelable(type, flags)
        parcel.writeByte(status.ordinal.toByte())
    }

    companion object CREATOR : Creator<Tag> {
        override fun createFromParcel(parcel: Parcel): Tag {
            return Tag(parcel)
        }

        override fun newArray(size: Int): Array<Tag?> {
            return arrayOfNulls(size)
        }
    }
}
