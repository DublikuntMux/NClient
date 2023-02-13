package com.dublikunt.nclientv2.classes

import android.content.Context
import android.net.Uri
import com.dublikunt.nclientv2.api.InspectorV3
import com.dublikunt.nclientv2.api.InspectorV3.InspectorResponse
import com.dublikunt.nclientv2.api.components.Tag
import com.dublikunt.nclientv2.api.enums.*
import com.dublikunt.nclientv2.async.database.Queries

class Bookmark(url: String, page: Int, requestType: ApiRequestType, tag: Int) {
    val url: String
    val page: Int
    val tag: Int
    private val requestType: ApiRequestType
    private val tagVal: Tag
    private val uri: Uri

    init {
        var tagVal1: Tag?
        this.url = url
        this.page = page
        this.requestType = requestType
        this.tag = tag
        tagVal1 = Queries.TagTable.getTagById(this.tag)
        if (tagVal1 == null) tagVal1 = Tag(
            "english",
            0,
            SpecialTagIds.LANGUAGE_ENGLISH.toInt(),
            TagType.LANGUAGE,
            TagStatus.DEFAULT
        )
        tagVal = tagVal1
        uri = Uri.parse(url)
    }

    fun createInspector(context: Context?, response: InspectorResponse?): InspectorV3? {
        val query = uri.getQueryParameter("q")
        val popular = SortType.findFromAddition(uri.getQueryParameter("sort"))
        if (requestType === ApiRequestType.FAVORITE) return InspectorV3.favoriteInspector(
            context,
            query,
            page,
            response
        )
        if (requestType === ApiRequestType.BYSEARCH) return InspectorV3.searchInspector(
            context,
            query,
            null,
            page,
            popular,
            null,
            response
        )
        if (requestType === ApiRequestType.BYALL) return InspectorV3.searchInspector(
            context,
            "",
            null,
            page,
            SortType.RECENT_ALL_TIME,
            null,
            response
        )
        return if (requestType === ApiRequestType.BYTAG) InspectorV3.searchInspector(
            context,
            "",
            setOf(tagVal),
            page,
            SortType.findFromAddition(url),
            null,
            response
        ) else null
    }

    fun deleteBookmark() {
        Queries.BookmarkTable.deleteBookmark(url)
    }

    override fun toString(): String {
        if (requestType === ApiRequestType.BYTAG) return tagVal.type.single + ": " + tagVal.name
        if (requestType === ApiRequestType.FAVORITE) return "Favorite"
        if (requestType === ApiRequestType.BYSEARCH) return "" + uri.getQueryParameter("q")
        return if (requestType === ApiRequestType.BYALL) "Main page" else "WTF"
    }
}
