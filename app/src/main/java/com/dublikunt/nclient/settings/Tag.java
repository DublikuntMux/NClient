package com.dublikunt.nclient.settings;

import android.content.Context;

import androidx.annotation.NonNull;

import com.dublikunt.nclient.api.enums.TagStatus;
import com.dublikunt.nclient.api.enums.TagType;
import com.dublikunt.nclient.async.database.Queries;

import org.jetbrains.annotations.Contract;

import java.util.List;
import java.util.Set;

public class Tag {
    public static final int MAXTAGS = 100;
    private static int minCount;
    private static boolean sortByName;

    @NonNull
    public static List<com.dublikunt.nclient.api.components.Tag> getTagSet(TagType type) {
        return Queries.TagTable.getAllTagOfType(type);
    }

    @NonNull
    public static List<com.dublikunt.nclient.api.components.Tag> getTagStatus(TagStatus status) {
        return Queries.TagTable.getAllStatus(status);
    }

    @NonNull
    public static String getQueryString(String query, @NonNull Set<com.dublikunt.nclient.api.components.Tag> all) {
        StringBuilder builder = new StringBuilder();
        for (com.dublikunt.nclient.api.components.Tag t : all)
            if (!query.contains(t.getName())) builder.append('+').append(t.toQueryTag());
        return builder.toString();
    }

    public static List<com.dublikunt.nclient.api.components.Tag> getListPrefer(boolean removeIgnoredGalleries) {
        return removeIgnoredGalleries ? Queries.TagTable.getAllFiltered() :
            Queries.TagTable.getAllStatus(TagStatus.ACCEPTED);
    }

    public static TagStatus updateStatus(@NonNull com.dublikunt.nclient.api.components.Tag t) {
        switch (t.getStatus()) {
            case ACCEPTED:
                t.setStatus(TagStatus.AVOIDED);
                break;
            case AVOIDED:
                t.setStatus(TagStatus.DEFAULT);
                break;
            case DEFAULT:
                t.setStatus(TagStatus.ACCEPTED);
                break;
        }
        if (Queries.TagTable.updateTag(t) == 1) return t.getStatus();
        throw new RuntimeException("Unable to update: " + t);

    }

    public static void resetAllStatus() {
        Queries.TagTable.resetAllStatus();
    }

    @Contract(pure = true)
    public static boolean containTag(com.dublikunt.nclient.api.components.Tag[] tags, com.dublikunt.nclient.api.components.Tag t) {
        for (com.dublikunt.nclient.api.components.Tag t1 : tags) if (t.equals(t1)) return true;
        return false;
    }


    public static TagStatus getStatus(com.dublikunt.nclient.api.components.Tag tag) {
        return Queries.TagTable.getStatus(tag);
    }


    public static boolean maxTagReached() {
        return getListPrefer(Global.removeAvoidedGalleries()).size() >= MAXTAGS;
    }

    public static void updateMinCount(@NonNull Context context, int min) {
        context.getSharedPreferences("ScrapedTags", 0).edit().putInt("min_count", minCount = min).apply();
    }

    public static void initMinCount(@NonNull Context context) {
        minCount = context.getSharedPreferences("ScrapedTags", 0).getInt("min_count", 25);
    }

    public static void initSortByName(@NonNull Context context) {
        sortByName = context.getSharedPreferences("ScrapedTags", 0).getBoolean("sort_by_name", false);
    }

    public static boolean updateSortByName(@NonNull Context context) {
        context.getSharedPreferences("ScrapedTags", 0).edit().putBoolean("sort_by_name", sortByName = !sortByName).apply();
        return sortByName;
    }

    public static boolean isSortedByName() {
        return sortByName;
    }

    public static int getMinCount() {
        return minCount;
    }

    @NonNull
    public static String getAvoidedTags() {
        StringBuilder builder = new StringBuilder();
        List<com.dublikunt.nclient.api.components.Tag> tags = Queries.TagTable.getAllStatus(TagStatus.AVOIDED);
        for (com.dublikunt.nclient.api.components.Tag t : tags)
            builder.append('+').append(t.toQueryTag(TagStatus.AVOIDED));
        return builder.toString();
    }
}
