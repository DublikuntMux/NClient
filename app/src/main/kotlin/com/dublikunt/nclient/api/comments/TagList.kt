package com.dublikunt.nclient.api.comments

import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import com.dublikunt.nclient.enums.TagType

open class TagList() : Parcelable {
    private val tagList = arrayOfNulls<Tags>(TagType.values.size)

    protected constructor(`in`: Parcel) : this() {
        val list = ArrayList<Tag>()
        `in`.readTypedList(list, Tag.CREATOR)
        addTags(list)
    }

    init {
        for (type in TagType.values) tagList[type.id.toInt()] = Tags()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeTypedList(allTagsList)
    }

    val allTagsSet: Set<Tag>
        get() {
            val tags = HashSet<Tag>()
            for (t in tagList) tags.addAll(t!!)
            return tags
        }
    val allTagsList: List<Tag>
        get() {
            val tags: MutableList<Tag> = ArrayList()
            for (t in tagList) tags.addAll(t!!)
            return tags
        }

    fun getCount(type: TagType): Int {
        return tagList[type.id.toInt()]!!.size
    }

    fun getTag(type: TagType, index: Int): Tag {
        return tagList[type.id.toInt()]!![index]
    }

    val totalCount: Int
        get() {
            var total = 0
            for (t in tagList) total += t!!.size
            return total
        }

    fun addTag(tag: Tag) {
        tagList[tag.type.id.toInt()]!!.add(tag)
    }

    fun addTags(tags: Collection<Tag>) {
        for (t in tags) addTag(t)
    }

    fun retrieveForType(type: TagType): Tags? {
        return tagList[type.id.toInt()]
    }

    fun getLenght(): Int {
        return tagList.size
    }

    open fun hasTag(tag: Tag): Boolean {
        return tagList[tag.type.id.toInt()]!!.contains(tag)
    }

    open fun hasTags(tags: Collection<Tag>): Boolean {
        for (tag in tags) {
            if (!hasTag(tag)) {
                return false
            }
        }
        return true
    }

    fun sort(comparator: Comparator<Tag>) {
        for (t in tagList) t!!.sortWith(comparator)
    }

    class Tags : ArrayList<Tag> {
        constructor(initialCapacity: Int) : super(initialCapacity)
        constructor()
        constructor(c: Collection<Tag>) : super(c)
    }

    companion object {
        @JvmField
        val CREATOR: Creator<TagList> = object : Creator<TagList> {
            override fun createFromParcel(`in`: Parcel): TagList {
                return TagList(`in`)
            }

            override fun newArray(size: Int): Array<TagList?> {
                return arrayOfNulls(size)
            }
        }
    }
}
