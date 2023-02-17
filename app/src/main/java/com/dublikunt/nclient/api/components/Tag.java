package com.dublikunt.nclient.api.components;


import android.os.Parcel;
import android.os.Parcelable;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.JsonWriter;

import com.dublikunt.nclient.enums.TagStatus;
import com.dublikunt.nclient.enums.TagType;
import com.dublikunt.nclient.utility.LogUtility;

import java.io.IOException;
import java.util.Locale;

@SuppressWarnings("unused")
public class Tag implements Parcelable {
    public static final Creator<Tag> CREATOR = new Creator<Tag>() {
        /**
        * Creates a Tag object from a Parcel. This is used to create an instance of the Tag class when it is passed to #create ( com. intellij. openmbean. parcel. Parcel )
        *
        * @param in - The Parcel to create the Tag from.
        *
        * @return The newly created Tag object or null if the input was null or not parsable by the Tag class
        */
        @Override
        public Tag createFromParcel(Parcel in) {
            return new Tag(in);
        }

        /**
        * Creates a new array of the specified size. The array is backed by this Tag object so changes in the array are reflected in this object and vice versa.
        *
        * @param size - the size of the new array to be created
        */
        @Override
        public Tag[] newArray(int size) {
            return new Tag[size];
        }
    };
    private String name;
    private int count, id;
    private TagType type;
    private TagStatus status = TagStatus.DEFAULT;

    public Tag(String text) {
        this.count = Integer.parseInt(text.substring(0, text.indexOf(',')));
        text = text.substring(text.indexOf(',') + 1);
        this.id = Integer.parseInt(text.substring(0, text.indexOf(',')));
        text = text.substring(text.indexOf(',') + 1);
        this.type = TagType.values[Integer.parseInt(text.substring(0, text.indexOf(',')))];
        this.name = text.substring(text.indexOf(',') + 1);
    }

    public Tag(String name, int count, int id, TagType type, TagStatus status) {
        this.name = name;
        this.count = count;
        this.id = id;
        this.type = type;
        this.status = status;
    }

    public Tag(JsonReader jr) throws IOException {
        jr.beginObject();
        // Reads the tag data from the stream.
        while (jr.peek() != JsonToken.END_OBJECT) {
            // Reads the next tag from the stream
            switch (jr.nextName()) {
                case "count":
                    count = jr.nextInt();
                    break;
                case "type":
                    type = TagType.typeByName(jr.nextString());
                    break;
                case "id":
                    id = jr.nextInt();
                    break;
                case "name":
                    name = jr.nextString();
                    break;
                case "url":
                    LogUtility.download("Tag URL: " + jr.nextString());
                    break;
                default:
                    jr.skipValue();
                    break;
            }
        }
        jr.endObject();
    }

    private Tag(Parcel in) {
        name = in.readString();
        count = in.readInt();
        id = in.readInt();
        type = in.readParcelable(TagType.class.getClassLoader());
        status = TagStatus.values()[in.readByte()];
    }

    /**
    * Returns a string representation of this tag. This is used to create query tags for a search result.
    *
    * @param status - the status of the tag. Can be AVOIDED or UNAVOIDED.
    *
    * @return a string representation of this tag for a query result ( as specified by TagStatus ). Note that the format of this string is " - type : name "
    */
    public String toQueryTag(TagStatus status) {
        StringBuilder builder = new StringBuilder();
        // Append a space to the builder.
        if (status == TagStatus.AVOIDED)
            builder.append('-');
        builder
            .append(type.getSingle())
            .append(':')
            .append('"')
            .append(name)
            .append('"');
        return builder.toString();
    }

    /**
    * Returns the query tag corresponding to this status. This method is used by #getStatus ( String ) to convert status to query tag.
    *
    *
    * @return the query tag corresponding to this status or null if there is no status associated with this status or if the status is
    */
    public String toQueryTag() {
        return toQueryTag(status);
    }

    /**
    * Returns the name of this entity. This is the entity's name in the form of a java. lang. String object.
    *
    *
    * @return the entity's name in the form of a java. lang. String object or null if there is no
    */
    public String getName() {
        return name;
    }

    /**
    * Returns the number of items in the list. This is a constant value and should be used in situations where you don't care about the order
    */
    public int getCount() {
        return count;
    }

    /**
    * Returns the status of this tag. This is used to determine if an event is in progress or not.
    *
    *
    * @return TagStatus object that represents the status of this tag ( null if not in progress ). Note that a tag may have multiple statuses
    */
    public TagStatus getStatus() {
        return status;
    }

    /**
    * Sets the status of this tag. Note that this is an alias for setStatus (). It's used to indicate that the tag is in a state other than ACTIVE or SUSPENDED.
    *
    * @param status - the TagStatus to set as this tag's
    */
    public void setStatus(TagStatus status) {
        this.status = status;
    }

    /**
    * Returns the id of this object. This is used to distinguish objects created by the class from objects created by the object factory
    */
    public int getId() {
        return id;
    }

    /**
    * Updates the status of this tag. This is called when we have received a response from the tag server to determine if it is acceptable or not.
    *
    *
    * @return the tag status to be used or null if not acceptable or no status was received in the time of
    */
    public TagStatus updateStatus() {
        // Set the tag status to AVOIDED.
        switch (status) {
            case AVOIDED:
                return status = TagStatus.DEFAULT;
            case DEFAULT:
                return status = TagStatus.ACCEPTED;
            case ACCEPTED:
                return status = TagStatus.AVOIDED;
        }
        return null;
    }

    /**
    * Writes this object to a JsonWriter. This is used to create a JSON representation of the object that can be stored in a file.
    *
    * @param writer - The JsonWriter to write to. Must not be null
    */
    void writeJson(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("count").value(count);
        writer.name("type").value(getTypeSingleName());
        writer.name("id").value(id);
        writer.name("name").value(name);
        writer.endObject();
    }

    /**
    * Returns the tag type. This is used to distinguish tags that are part of a multi - tag set such as an ID tag with ID = 0 and a tag with ID = 1.
    *
    *
    * @return the tag type or TagType#NONE if there is no tag type or the tag is a multivalued
    */
    public TagType getType() {
        return type;
    }

    /**
    * Returns the name of the type that this property represents. This is used to distinguish between " complex " and " complex_multi " types.
    *
    *
    * @return the name of the type that this property represents or null if there is no type with that name ( such as " complex "
    */
    public String getTypeSingleName() {
        return type.getSingle();
    }


    /**
    * Returns a String representation of this Tag. This is used for debugging purposes and the content of the Tag may vary between implementations.
    *
    *
    * @return a String representation of this Tag in the form name = #name \'count = #count id = #id type = #type
    */
    @Override
    public String toString() {
        return "Tag{" +
            "name='" + name + '\'' +
            ", count=" + count +
            ", id=" + id +
            ", type=" + type +
            ", status=" + status +
            '}';
    }

    /**
    * Returns a scraped representation of this message. The format is : count id type id name ( without spaces )
    *
    *
    * @return a string representation of this message suitable for debugging or debugging with java. lang. String#format ( java. lang. String
    */
    public String toScrapedString() {
        return String.format(Locale.US, "%d,%d,%d,%s", count, id, type.getId(), name);
    }

    /**
    * Returns true if this tag is equal to the specified object. Two tags are equal if they have the same class and their ID are equal
    *
    * @param o - the object to compare
    */
    @Override
    public boolean equals(Object o) {
        // Returns true if this object is the same as the receiver.
        if (this == o) return true;
        // Returns true if this object is a subclass of the same class.
        if (o == null || getClass() != o.getClass()) return false;

        Tag tag = (Tag) o;

        return id == tag.id;
    }

    /**
    * Returns hash code for this object. This is used to avoid collisions between objects that are created and deleted
    */
    @Override
    public int hashCode() {
        return id;
    }

    /**
    * Returns 0 for no contents 1 for content and 2 for content. This is used to determine whether or not a file is in use
    */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
    * Writes the data associated with this object to the parcel. This method is called by the protocol layer to write the data associated with this object to the parcel.
    *
    * @param parcel - The parcel to write to. Must not be null.
    * @param flags - Additional flags to use when writing the object. May be 0
    */
    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(name);
        parcel.writeInt(count);
        parcel.writeInt(id);
        parcel.writeParcelable(type, flags);
        parcel.writeByte((byte) status.ordinal());
    }
}
