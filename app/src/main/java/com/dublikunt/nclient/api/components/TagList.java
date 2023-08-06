package com.dublikunt.nclient.api.components;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.dublikunt.nclient.api.enums.TagType;

import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TagList implements Parcelable {
    public static final Creator<TagList> CREATOR = new Creator<>() {
        @NonNull
        @Contract("_ -> new")
        @Override
        public TagList createFromParcel(Parcel in) {
            return new TagList(in);
        }

        @NonNull
        @Contract(value = "_ -> new", pure = true)
        @Override
        public TagList[] newArray(int size) {
            return new TagList[size];
        }
    };
    private final Tags[] tagList = new Tags[TagType.values.length];

    protected TagList(@NonNull Parcel in) {
        this();
        ArrayList<Tag> list = new ArrayList<>();
        in.readTypedList(list, Tag.CREATOR);
        addTags(list);
    }

    public TagList() {
        for (TagType type : TagType.values) tagList[type.getId()] = new Tags();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeTypedList(getAllTagsList());
    }

    public Set<Tag> getAllTagsSet() {
        HashSet<Tag> tags = new HashSet<>();
        for (Tags t : tagList) tags.addAll(t);
        return tags;
    }

    public List<Tag> getAllTagsList() {
        List<Tag> tags = new ArrayList<>();
        for (Tags t : tagList) tags.addAll(t);
        return tags;
    }

    public int getCount(@NonNull TagType type) {
        return tagList[type.getId()].size();
    }

    public Tag getTag(@NonNull TagType type, int index) {
        return tagList[type.getId()].get(index);
    }

    public int getTotalCount() {
        int total = 0;
        for (Tags t : tagList) total += t.size();
        return total;
    }

    public void addTag(@NonNull Tag tag) {
        tagList[tag.getType().getId()].add(tag);
    }

    public void addTags(@NonNull Collection<? extends Tag> tags) {
        for (Tag t : tags) addTag(t);
    }

    public List<Tag> retrieveForType(@NonNull TagType type) {
        return tagList[type.getId()];
    }

    public int getLenght() {
        return tagList.length;
    }

    public void sort(Comparator<Tag> comparator) {
        for (Tags t : tagList) t.sort(comparator);
    }

    public boolean hasTag(@NonNull Tag tag) {
        return tagList[tag.getType().getId()].contains(tag);
    }

    public boolean hasTags(@NonNull Collection<Tag> tags) {
        for (Tag tag : tags) {
            if (!hasTag(tag)) {
                return false;
            }
        }
        return true;
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
