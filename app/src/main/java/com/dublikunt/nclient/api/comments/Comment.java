package com.dublikunt.nclient.api.comments;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.JsonReader;
import android.util.JsonToken;

import java.io.IOException;
import java.util.Date;

public class Comment implements Parcelable {
    public static final Creator<Comment> CREATOR = new Creator<Comment>() {
        /**
         * Creates a new instance of this class from a Parcel. This is used to create comments that are part of a comment - like object such as a list of comments or an object that has been added to a collection.
         *
         * @param in - The Parcel to create the comment from.
         *
         * @return A new instance of this class with the data from the Parcel passed in. Note that the type of the object is determined by the implementation
         */
        @Override
        public Comment createFromParcel(Parcel in) {
            return new Comment(in);
        }

        /**
         * Creates a new array of the specified size. The array is backed by this object so changes in one will affect the other.
         *
         * @param size - the size of the new array to be created
         */
        @Override
        public Comment[] newArray(int size) {
            return new Comment[size];
        }
    };
    private int id;
    private User poster;
    private Date postDate;
    private String body;

    public Comment(JsonReader reader) throws IOException {
        reader.beginObject();
        // Reads the next object from the stream.
        while (reader.peek() != JsonToken.END_OBJECT) {
            // Reads the next value from the stream.
            switch (reader.nextName()) {
                case "id":
                    id = reader.nextInt();
                    break;
                case "post_date":
                    postDate = new Date(reader.nextLong() * 1000);
                    break;
                case "body":
                    body = reader.nextString();
                    break;
                case "poster":
                    poster = new User(reader);
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();
    }

    protected Comment(Parcel in) {
        id = in.readInt();
        poster = in.readParcelable(User.class.getClassLoader());
        body = in.readString();
        postDate = new Date(in.readLong());
    }

    /**
     * Returns 0 for no contents 1 for content and 2 for content. This is used to determine whether or not a file is in use
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Writes this object to a Parcel. This will be called by the OperatingSystem when it is serializing the object.
     *
     * @param dest  - The Parcel to which the object is to be written.
     * @param flags - Additional flags about how the object should be written
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeParcelable(poster, flags);
        dest.writeString(body);
        dest.writeLong(postDate.getTime());
    }

    /**
     * Returns the id of this object. This is used to distinguish objects created by the class from objects created by the object factory
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the poster of this message. This may be null if the message is an unposted message.
     *
     * @return the poster of this message or null if the message is not a poster of this message or if there is no
     */
    public User getPoster() {
        return poster;
    }

    /**
     * Returns the post date. This is used to determine when the user is posting or not. If the user isn't posting it will return null.
     *
     * @return the post date or null if the user isn't posting or not in the system time zone
     */
    public Date getPostDate() {
        return postDate;
    }

    /**
     * Returns the body of the comment. This is used to determine if there is a comment in the comment field or not.
     *
     * @return the body of the comment or null if there is no comment in the comment field or if the comment is
     */
    public String getComment() {
        return body;
    }

    /**
     * Returns the poster's ID. This is used to distinguish posters from other posters when they have a lot of information
     */
    public int getPosterId() {
        return poster.getId();
    }

    /**
     * Returns the username of the poster. This is used to check if the user is logged in and if so what's going on to be the username that is stored in the database.
     *
     * @return the username of the poster or null if there is no user logged in in the database or if the poster is
     */
    public String getUsername() {
        return poster.getUsername();
    }

    /**
     * Returns the URL of the poster's avatar. Note that this will return null if there is no avatar.
     *
     * @return the URL of the poster's avatar or null if there is no avatar in the poster
     */
    public Uri getAvatarUrl() {
        return poster.getAvatarUrl();
    }
}
