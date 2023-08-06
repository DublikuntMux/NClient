package com.dublikunt.nclient.api.comments;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.JsonReader;
import android.util.JsonToken;

import androidx.annotation.NonNull;

import com.dublikunt.nclient.utility.Utility;

import org.jetbrains.annotations.Contract;

import java.io.IOException;
import java.util.Locale;

public class User implements Parcelable {
    public static final Creator<User> CREATOR = new Creator<>() {
        @NonNull
        @Contract("_ -> new")
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @NonNull
        @Contract(value = "_ -> new", pure = true)
        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };
    private int id;
    private String username, avatarUrl;

    public User(@NonNull JsonReader reader) throws IOException {
        reader.beginObject();
        while (reader.peek() != JsonToken.END_OBJECT) {
            switch (reader.nextName()) {
                case "id":
                    id = reader.nextInt();
                    break;
                case "post_date":
                    username = reader.nextString();
                    break;
                case "avatar_url":
                    avatarUrl = reader.nextString();
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();
    }

    protected User(@NonNull Parcel in) {
        id = in.readInt();
        username = in.readString();
        avatarUrl = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(username);
        dest.writeString(avatarUrl);
    }

    public int getId() {
        return id;
    }

    public Uri getAvatarUrl() {
        return Uri.parse(String.format(Locale.US, "https://i.%s/%s", Utility.getHost(), avatarUrl));
    }

    public String getUsername() {
        return username;
    }
}
