package com.dublikunt.nclient.api.comments;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.JsonReader;
import android.util.JsonToken;

import com.dublikunt.nclient.utility.Utility;

import java.io.IOException;
import java.util.Locale;

public class User implements Parcelable {
    public static final Creator<User> CREATOR = new Creator<User>() {
        /**
         * Creates a user from a parcel. This is used to create user objects that are part of the data that is stored in the parcel.
         *
         * @param in - The parcel containing the user data. Must not be null.
         *
         * @return The user created from the parcel. Never null but may be the same instance as the parameter if there is a problem
         */
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        /**
         * Creates a new array of the specified size. Note that the size of the array is specified in terms of the number of elements that can be stored in the array.
         *
         * @param size - the size of the new array to be created
         */
        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };
    private int id;
    private String username, avatarUrl;

    public User(JsonReader reader) throws IOException {
        reader.beginObject();
        // Reads the next object from the stream.
        while (reader.peek() != JsonToken.END_OBJECT) {
            // Reads the next value from the reader.
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

    protected User(Parcel in) {
        id = in.readInt();
        username = in.readString();
        avatarUrl = in.readString();
    }

    /**
     * Returns 0 for no contents 1 for content and 2 for content. This is used to determine whether or not a file is in use
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Writes the object to a Parcel. This will be written as a four - byte integer followed by the username and avatarUrl.
     *
     * @param dest  - The com. google. gwt. user. client. parcel. Parcel to which the object is to be written.
     * @param flags - Additional flags about how the object should be written
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(username);
        dest.writeString(avatarUrl);
    }

    /**
     * Returns the id of this object. This is used to distinguish objects created by the class from objects created by the object factory
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the URL of the avatar. This is based on the host and avatarUrl. It does not include the scheme but the host is included for convenience.
     *
     * @return The URL of the avatar or null if there is no avatar to display in the web browser ( not a server
     */
    public Uri getAvatarUrl() {
        return Uri.parse(String.format(Locale.US, "https://i.%s/%s", Utility.getHost(), avatarUrl));
    }

    /**
     * Returns the username associated with this user. This is a String that can be used to create a username for an API call.
     *
     * @return the username associated with this user or null if there is no username associated with this user or if the username is
     */
    public String getUsername() {
        return username;
    }
}
