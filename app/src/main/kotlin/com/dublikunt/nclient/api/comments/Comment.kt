package com.dublikunt.nclient.api.comments

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import android.util.JsonReader
import android.util.JsonToken
import java.util.*

open class Comment : Parcelable {
    var id = 0
        private set
    private lateinit var poster: User
    lateinit var postDate: Date
        private set
    lateinit var comment: String
        private set

    constructor(reader: JsonReader) {
        reader.beginObject()
        while (reader.peek() != JsonToken.END_OBJECT) {
            when (reader.nextName()) {
                "id" -> id = reader.nextInt()
                "post_date" -> postDate = Date(reader.nextLong() * 1000)
                "body" -> comment = reader.nextString()
                "poster" -> poster = User(reader)
                else -> reader.skipValue()
            }
        }
        reader.endObject()
    }

    protected constructor(`in`: Parcel) {
        id = `in`.readInt()
        poster = `in`.readParcelable(User::class.java.classLoader)!!
        comment = `in`.readString().toString()
        postDate = Date(`in`.readLong())
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(id)
        dest.writeParcelable(poster, flags)
        dest.writeString(comment)
        dest.writeLong(postDate.time)
    }

    val posterId: Int
        get() = poster.id
    val username: String
        get() = poster.username
    val avatarUrl: Uri
        get() = poster.getAvatarUrl()

    companion object {
        @JvmField
        val CREATOR: Creator<Comment> = object : Creator<Comment> {
            override fun createFromParcel(`in`: Parcel): Comment {
                return Comment(`in`)
            }

            override fun newArray(size: Int): Array<Comment?> {
                return arrayOfNulls(size)
            }
        }
    }
}
