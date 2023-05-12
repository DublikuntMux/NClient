package com.dublikunt.nclient.api.comments

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import android.util.JsonReader
import android.util.JsonToken
import com.dublikunt.nclient.utility.Utility.host
import java.util.Locale

open class User : Parcelable {
    var id = 0
        private set
    var username: String = ""
        private set
    private var avatarUrl: String = ""

    constructor(reader: JsonReader) {
        reader.beginObject()
        while (reader.peek() != JsonToken.END_OBJECT) {
            when (reader.nextName()) {
                "id" -> id = reader.nextInt()
                "post_date" -> username = reader.nextString()
                "avatar_url" -> avatarUrl = reader.nextString()
                else -> reader.skipValue()
            }
        }
        reader.endObject()
    }

    protected constructor(`in`: Parcel) {
        id = `in`.readInt()
        username = `in`.readString().toString()
        avatarUrl = `in`.readString().toString()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(id)
        dest.writeString(username)
        dest.writeString(avatarUrl)
    }

    fun getAvatarUrl(): Uri {
        return Uri.parse(String.format(Locale.US, "https://i.%s/%s", host, avatarUrl))
    }

    companion object CREATOR : Creator<User> {
        override fun createFromParcel(parcel: Parcel): User {
            return User(parcel)
        }

        override fun newArray(size: Int): Array<User?> {
            return arrayOfNulls(size)
        }
    }
}
