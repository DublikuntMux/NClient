package com.dublikunt.nclient.enums

import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator

open class TagType : Parcelable {
    val id: Byte
    val single: String
    private val plural: String?

    private constructor(id: Int, single: String, plural: String?) {
        this.id = id.toByte()
        this.single = single
        this.plural = plural
    }

    protected constructor(`in`: Parcel) {
        id = `in`.readByte()
        single = `in`.readString().toString()
        plural = `in`.readString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val type = other as TagType
        return id == type.id
    }

    override fun hashCode(): Int {
        return id.toInt()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeByte(id)
        dest.writeString(single)
        dest.writeString(plural)
    }

    companion object CREATOR : Creator<TagType> {
        val UNKNOWN = TagType(0, "", null)
        val PARODY = TagType(1, "parody", "parodies")
        val CHARACTER = TagType(2, "character", "characters")
        val TAG = TagType(3, "tag", "tags")
        val ARTIST = TagType(4, "artist", "artists")
        val GROUP = TagType(5, "group", "groups")
        val LANGUAGE = TagType(6, "language", null)
        val CATEGORY = TagType(7, "category", null)
        val values = arrayOf(UNKNOWN, PARODY, CHARACTER, TAG, ARTIST, GROUP, LANGUAGE, CATEGORY)

        override fun createFromParcel(parcel: Parcel): TagType {
            return TagType(parcel)
        }

        override fun newArray(size: Int): Array<TagType?> {
            return arrayOfNulls(size)
        }

        fun typeByName(name: String): TagType {
            for (t in values) if (t.single == name) return t
            return UNKNOWN
        }
    }
}
