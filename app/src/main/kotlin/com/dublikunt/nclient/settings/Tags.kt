package com.dublikunt.nclient.settings

import android.content.Context
import com.dublikunt.nclient.api.components.Tag
import com.dublikunt.nclient.enums.TagStatus
import com.dublikunt.nclient.enums.TagType
import com.dublikunt.nclient.async.database.Queries
import com.dublikunt.nclient.settings.Global.removeAvoidedGalleries

object Tags {
    const val MAXTAGS = 100

    @JvmStatic
    var minCount = 0
        private set

    @JvmStatic
    var isSortedByName = false
        private set

    fun getTagSet(type: TagType): List<Tag> {
        return Queries.TagTable.getAllTagOfType(type)
    }

    fun getTagStatus(status: TagStatus): List<Tag> {
        return Queries.TagTable.getAllStatus(status)
    }

    fun getQueryString(query: String, all: Set<Tag>): String {
        val builder = StringBuilder()
        for (t in all) if (!query.contains(t.name)) builder.append('+').append(t.toQueryTag())
        return builder.toString()
    }

    fun getListPrefer(removeIgnoredGalleries: Boolean): List<Tag> {
        return if (removeIgnoredGalleries) Queries.TagTable.allFiltered else Queries.TagTable.getAllStatus(
            TagStatus.ACCEPTED
        )
    }

    @JvmStatic
    fun updateStatus(t: Tag): TagStatus {
        when (t.status) {
            TagStatus.ACCEPTED -> t.status = TagStatus.AVOIDED
            TagStatus.AVOIDED -> t.status = TagStatus.DEFAULT
            TagStatus.DEFAULT -> t.status = TagStatus.ACCEPTED
        }
        if (Queries.TagTable.updateTag(t) == 1) return t.status
        throw RuntimeException("Unable to update: $t")
    }

    fun resetAllStatus() {
        Queries.TagTable.resetAllStatus()
    }

    fun containTag(tags: Array<Tag>, t: Tag): Boolean {
        for (t1 in tags) if (t == t1) return true
        return false
    }

    fun getStatus(tag: Tag): TagStatus? {
        return Queries.TagTable.getStatus(tag)
    }

    @JvmStatic
    fun maxTagReached(): Boolean {
        return getListPrefer(removeAvoidedGalleries()).size >= MAXTAGS
    }

    fun updateMinCount(context: Context, min: Int) {
        context.getSharedPreferences("ScrapedTags", 0).edit()
            .putInt("min_count", min.also { minCount = it }).apply()
    }

    fun initMinCount(context: Context) {
        minCount = context.getSharedPreferences("ScrapedTags", 0).getInt("min_count", 25)
    }

    fun initSortByName(context: Context) {
        isSortedByName =
            context.getSharedPreferences("ScrapedTags", 0).getBoolean("sort_by_name", false)
    }

    fun updateSortByName(context: Context): Boolean {
        context.getSharedPreferences("ScrapedTags", 0).edit()
            .putBoolean("sort_by_name", !isSortedByName.also { isSortedByName = it }).apply()
        return isSortedByName
    }

    val avoidedTags: String
        get() {
            val builder = StringBuilder()
            val tags = Queries.TagTable.getAllStatus(TagStatus.AVOIDED)
            for (t in tags) builder.append('+').append(t.toQueryTag(TagStatus.AVOIDED))
            return builder.toString()
        }
}
