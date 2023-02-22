package com.dublikunt.nclient.api.components;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.dublikunt.nclient.enums.TagType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TagList implements Parcelable {

    public static final Creator<TagList> CREATOR = new Creator<TagList>() {
        /**
         * Creates a TagList from a Parcel. This is used to create an instance of the tag list for the purpose of testing.
         *
         * @param in - The Parcel to parse. Must not be null.
         *
         * @return The newly created object. Never null but may be populated with data from the Parcel passed in or from other sources
         */
        @Override
        public TagList createFromParcel(Parcel in) {
            return new TagList(in);
        }

        /**
         * Creates a new array of TagList. The size of the array must be greater than or equal to the number of tags in this TagList.
         *
         * @param size - the size of the new array to be returned
         */
        @Override
        public TagList[] newArray(int size) {
            return new TagList[size];
        }
    };
    private final Tags[] tagList = new Tags[TagType.values.length];

    protected TagList(Parcel in) {
        this();
        ArrayList<Tag> list = new ArrayList<>();
        in.readTypedList(list, Tag.CREATOR);
        addTags(list);
    }

    public TagList() {
        for (TagType type : TagType.values) tagList[type.getId()] = new Tags();
    }

    /**
     * Returns 0 for no contents 1 for content and 2 for content. This is used to determine whether or not a file is in use
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Writes the tag to a Parcel. This will be written as a list of tag - value pairs where the key is the tag name and the value is the value that should be written to the parcel
     *
     * @param dest  - The Parcel to which the tag - value pairs are to be written
     * @param flags - Additional flags about how the object should be written
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(getAllTagsList());
    }

    /**
     * Returns all tags in this tag set. This is a set of all tags that have been added to this tag
     */
    public Set<Tag> getAllTagsSet() {
        HashSet<Tag> tags = new HashSet<>();
        for (Tags t : tagList) tags.addAll(t);
        return tags;
    }

    /**
     * Returns all tags in the tag list. This is useful for debugging and to avoid having to iterate over the tags
     */
    public List<Tag> getAllTagsList() {
        List<Tag> tags = new ArrayList<>();
        for (Tags t : tagList) tags.addAll(t);
        return tags;
    }

    /**
     * Returns the number of tags of the given type. This is used to determine how many times a tag is in the list.
     *
     * @param type - the tag type to look up ( must be a TagType
     */
    public int getCount(TagType type) {
        return tagList[type.getId()].size();
    }

    /**
     * Returns the tag with the specified type and index. This method is equivalent to #getTag ( TagType int )
     *
     * @param type  - the type of tag to return
     * @param index - the index of the tag to return. 0 is the first tag 1 the second and so on
     * @return the tag with the specified type and index or null if there is no tag with that type or index
     */
    public Tag getTag(TagType type, int index) {
        return tagList[type.getId()].get(index);
    }

    /**
     * Returns the total number of tags in this collection. This is used to determine how many tags are present
     */
    public int getTotalCount() {
        int total = 0;
        for (Tags t : tagList) total += t.size();
        return total;
    }

    /**
     * Adds a tag to the list of tags. This is used to determine which tags are part of the document
     *
     * @param tag - The tag to be
     */
    public void addTag(Tag tag) {
        tagList[tag.getType().getId()].add(tag);
    }

    /**
     * Adds a collection of tags to the tag set. This is useful for adding tags to an existing tag set that is in the process of being merged into a new tag set.
     *
     * @param tags - the tags to be added to the tag set
     */
    public void addTags(Collection<? extends Tag> tags) {
        for (Tag t : tags) addTag(t);
    }

    /**
     * Retrieves all tags for a given type. This is useful for determining which tags to retrieve based on the type of tag you are looking for
     *
     * @param type - the type of tag
     */
    public List<Tag> retrieveForType(TagType type) {
        return tagList[type.getId()];
    }

    /**
     * Returns the number of tags in the tag list. This is the same as the length of the tag
     */
    public int getLenght() {
        return tagList.length;
    }

    /**
     * Sorts the tags in this TagList according to the given comparator. This is useful for sorting tag lists that are in the order they were added to the tag list.
     *
     * @param comparator - the comparator to use for sorting the tags
     */
    public void sort(Comparator<Tag> comparator) {
        for (Tags t : tagList) t.sort(comparator);
    }

    public static class Tags extends ArrayList<Tag> {
        public Tags(int initialCapacity) {
            super(initialCapacity);
        }

        public Tags() {
        }

        public Tags(@NonNull Collection<? extends Tag> c) {
            super(c);
        }
    }
}
