package com.dublikunt.nclient.components.classes;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.dublikunt.nclient.api.Inspector;
import com.dublikunt.nclient.api.components.Tag;
import com.dublikunt.nclient.api.enums.ApiRequestType;
import com.dublikunt.nclient.api.enums.SortType;
import com.dublikunt.nclient.api.enums.SpecialTagIds;
import com.dublikunt.nclient.api.enums.TagStatus;
import com.dublikunt.nclient.api.enums.TagType;
import com.dublikunt.nclient.async.database.Queries;

import java.util.Collections;

public class Bookmark {
    public final String url;
    public final int page, tag;
    private final ApiRequestType requestType;
    private final Tag tagVal;
    private final Uri uri;

    public Bookmark(String url, int page, ApiRequestType requestType, int tag) {
        Tag tagVal1;
        this.url = url;
        this.page = page;
        this.requestType = requestType;
        this.tag = tag;
        tagVal1 = Queries.TagTable.getTagById(this.tag);
        if (tagVal1 == null)
            tagVal1 = new Tag("english", 0, SpecialTagIds.LANGUAGE_ENGLISH, TagType.LANGUAGE, TagStatus.DEFAULT);
        this.tagVal = tagVal1;
        this.uri = Uri.parse(url);
    }

    public Inspector createInspector(Context context, Inspector.InspectorResponse response) {
        String query = uri.getQueryParameter("q");
        SortType popular = SortType.findFromAddition(uri.getQueryParameter("sort"));
        if (requestType == ApiRequestType.FAVORITE)
            return Inspector.favoriteInspector(context, query, page, response);
        if (requestType == ApiRequestType.BYSEARCH)
            return Inspector.searchInspector(context, query, null, page, popular, null, response);
        if (requestType == ApiRequestType.BYALL)
            return Inspector.searchInspector(context, "", null, page, SortType.RECENT_ALL_TIME, null, response);
        if (requestType == ApiRequestType.BYTAG) return Inspector.searchInspector(context, "",
            Collections.singleton(tagVal), page, SortType.findFromAddition(this.url), null, response);
        return null;
    }

    public void deleteBookmark() {
        Queries.BookmarkTable.deleteBookmark(url);
    }

    @NonNull
    @Override
    public String toString() {
        if (requestType == ApiRequestType.BYTAG)
            return tagVal.getType().getSingle() + ": " + tagVal.getName();
        if (requestType == ApiRequestType.FAVORITE) return "Favorite";
        if (requestType == ApiRequestType.BYSEARCH) return "" + uri.getQueryParameter("q");
        if (requestType == ApiRequestType.BYALL) return "Main page";
        return "WTF";
    }
}
