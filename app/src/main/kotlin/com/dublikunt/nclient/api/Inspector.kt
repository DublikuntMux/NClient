package com.dublikunt.nclient.api

import android.content.Context
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import com.dublikunt.nclient.api.components.Gallery
import com.dublikunt.nclient.api.components.GenericGallery
import com.dublikunt.nclient.api.components.Ranges
import com.dublikunt.nclient.api.components.Tag
import com.dublikunt.nclient.async.database.Queries.TagTable.allOnlineBlacklisted
import com.dublikunt.nclient.async.database.Queries.TagTable.getAllStatus
import com.dublikunt.nclient.async.database.Queries.TagTable.getTagById
import com.dublikunt.nclient.enums.*
import com.dublikunt.nclient.settings.Global.client
import com.dublikunt.nclient.settings.Global.getOnlyLanguage
import com.dublikunt.nclient.settings.Global.isOnlyTag
import com.dublikunt.nclient.settings.Global.removeAvoidedGalleries
import com.dublikunt.nclient.settings.Global.sortType
import com.dublikunt.nclient.utility.LogUtility.download
import com.dublikunt.nclient.utility.LogUtility.error
import com.dublikunt.nclient.utility.Utility.baseUrl
import com.dublikunt.nclient.utility.Utility.unescapeUnicodeString
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.*

open class Inspector : Thread, Parcelable {
    private var sortType: SortType? = null
    var isCustom = false
        private set
    private var forceStart = false
    var page = 0
    var pageCount = -1
        private set
    private var id = 0
    lateinit var query: String
        private set
    lateinit var url: String
        private set
    lateinit var requestType: ApiRequestType
        private set
    private lateinit var tags: Collection<Tag>
    lateinit var galleries: ArrayList<GenericGallery>
    private var ranges: Ranges? = null
    var response: InspectorResponse? = null
        private set
    private var context: WeakReference<Context>? = null
    private var htmlDocument: Document? = null

    protected constructor(`in`: Parcel) {
        sortType = SortType.values()[`in`.readByte().toInt()]
        isCustom = `in`.readByte().toInt() != 0
        page = `in`.readInt()
        pageCount = `in`.readInt()
        id = `in`.readInt()
        query = `in`.readString().toString()
        url = `in`.readString().toString()
        requestType = ApiRequestType.values[`in`.readByte().toInt()]
        val x: ArrayList<*>? = when (GenericGallery.Type.values()[`in`.readByte().toInt()]) {
            GenericGallery.Type.LOCAL -> `in`.createTypedArrayList(
                LocalGallery.CREATOR
            )
            GenericGallery.Type.SIMPLE -> `in`.createTypedArrayList(
                SimpleGallery.CREATOR
            )
            GenericGallery.Type.COMPLETE -> `in`.createTypedArrayList(
                Gallery.CREATOR
            )
        }
        galleries = x as ArrayList<GenericGallery>
        tags = HashSet(`in`.createTypedArrayList(Tag.CREATOR))
        ranges = `in`.readParcelable(Ranges::class.java.classLoader)
    }

    private constructor(context: Context, response: InspectorResponse?) {
        initialize(context, response)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeByte(
            Objects.requireNonNullElse(
                sortType,
                SortType.RECENT_ALL_TIME
            )!!.ordinal.toByte()
        )
        dest.writeByte((if (isCustom) 1 else 0).toByte())
        dest.writeInt(page)
        dest.writeInt(pageCount)
        dest.writeInt(id)
        dest.writeString(query)
        dest.writeString(url)
        dest.writeByte(requestType.ordinal())
        if (galleries.size == 0) dest.writeByte(GenericGallery.Type.SIMPLE.ordinal.toByte()) else dest.writeByte(
            galleries[0].type.ordinal.toByte()
        )
        dest.writeTypedList(galleries)
        dest.writeTypedList(ArrayList(tags))
        dest.writeParcelable(ranges, flags)
    }

    val searchTitle: String
        get() = query.ifEmpty {
            url.replace(
                baseUrl + "search/?q=",
                ""
            ).replace('+', ' ')
        }

    fun initialize(context: Context, response: InspectorResponse?) {
        this.response = response
        this.context = WeakReference(context)
    }

    fun cloneInspector(context: Context, response: InspectorResponse?): Inspector {
        val inspector = Inspector(context, response)
        inspector.query = query
        inspector.url = url
        inspector.tags = tags
        inspector.requestType = requestType
        inspector.sortType = sortType
        inspector.pageCount = pageCount
        inspector.page = page
        inspector.id = id
        inspector.isCustom = isCustom
        inspector.ranges = ranges
        return inspector
    }

    private fun tryByAllPopular() {
        if (sortType !== SortType.RECENT_ALL_TIME) {
            requestType = ApiRequestType.BYSEARCH
            query = "-nclient"
        }
    }

    private fun createUrl() {
        val builder = StringBuilder(baseUrl)
        if (requestType === ApiRequestType.BYALL) builder.append("?page=")
            .append(page) else if (requestType === ApiRequestType.RANDOM) builder.append("random/") else if (requestType === ApiRequestType.RANDOM_FAVORITE) builder.append(
            "favorites/random"
        ) else if (requestType === ApiRequestType.BYSINGLE) builder.append("g/")
            .append(id) else if (requestType === ApiRequestType.FAVORITE) {
            builder.append("favorites/")
            if (query.isNotEmpty()) builder.append("?q=").append(query)
                .append('&') else builder.append('?')
            builder.append("page=").append(page)
        } else if (requestType === ApiRequestType.BYSEARCH || requestType === ApiRequestType.BYTAG) {
            builder.append("search/?q=").append(query)
            for (tt in tags) {
                if (builder.toString().contains(tt.toQueryTag(TagStatus.ACCEPTED))) continue
                builder.append('+').append(tt.toQueryTag())
            }
            if (ranges != null) builder.append('+').append(ranges!!.toQuery())
            builder.append("&page=").append(page)
            if (sortType!!.urlAddition != null) {
                builder.append("&sort=").append(sortType!!.urlAddition)
            }
        }
        url = builder.toString().replace(' ', '+')
        download("WWW: $bookmarkURL")
    }

    fun forceStart() {
        forceStart = true
        start()
    }

    private val bookmarkURL: String
        get() = if (page < 2) url else url.substring(0, url.lastIndexOf('=') + 1)

    @Throws(IOException::class)
    fun createDocument() {
        if (htmlDocument != null) return
        val response =
            Objects.requireNonNull<OkHttpClient?>(client)
                .newCall(Request.Builder().url(url).build())
                .execute()
        htmlDocument = Jsoup.parse(response.body.byteStream(), "UTF-8", baseUrl)
        response.close()
    }

    @Throws(IOException::class, InvalidResponseException::class)
    fun parseDocument() {
        if (requestType.isSingle) doSingle(htmlDocument!!.body()) else doSearch(htmlDocument!!.body())
        htmlDocument = null
    }

    fun canParseDocument(): Boolean {
        return htmlDocument != null
    }

    @Synchronized
    override fun start() {
        if (state != State.NEW) return
        if (forceStart || response!!.shouldStart(this)) super.start()
    }

    override fun run() {
        download("Starting download: $url")
        if (response != null) response!!.onStart()
        try {
            createDocument()
            parseDocument()
            if (response != null) {
                response!!.onSuccess(galleries)
            }
        } catch (e: Exception) {
            if (response != null) response!!.onFailure(e)
        }
        if (response != null) response!!.onEnd()
        download("Finished download: $url")
    }

    @Throws(InvalidResponseException::class)
    private fun doSingle(document: Element) {
        galleries = ArrayList(1)
        val scripts = document.getElementsByTag("script")
        if (scripts.size == 0) throw InvalidResponseException()
        val json = trimScriptTag(Objects.requireNonNull(scripts.last())!!.html())
            ?: throw InvalidResponseException()
        val relContainer = document.getElementById("related-container")
        val rel: Elements =
            if (relContainer != null) relContainer.getElementsByClass("gallery") else Elements()
        val isFavorite: Boolean = try {
            document.getElementById("favorite")
                ?.getElementsByTag("span")!![0].text() == "Unfavorite"
        } catch (e: Exception) {
            false
        }
        download("is favorite? $isFavorite")
        galleries.add(Gallery(context!!.get(), json, rel, isFavorite))
    }

    private fun trimScriptTag(scriptHtml: String): String? {
        var scriptHtml = scriptHtml
        var s = scriptHtml.indexOf("parse")
        if (s < 0) return null
        s += 7
        scriptHtml = scriptHtml.substring(s, scriptHtml.lastIndexOf(");") - 1)
        scriptHtml = unescapeUnicodeString(scriptHtml)
        return scriptHtml.ifEmpty { null }
    }

    @Throws(InvalidResponseException::class)
    private fun doSearch(document: Element) {
        var gal = document.getElementsByClass("gallery")
        galleries = ArrayList(gal.size)
        for (e in gal) galleries.add(SimpleGallery(context!!.get(), e))
        gal = document.getElementsByClass("last")
        pageCount =
            if (gal.size == 0) 1.coerceAtLeast(page) else findTotal(gal.last()!!)
        if (gal.size == 0 && pageCount == 1 && document.getElementById("content") == null) throw InvalidResponseException()
    }

    private fun findTotal(e: Element): Int {
        val temp = e.attr("href")
        return try {
            Uri.parse(temp).getQueryParameter("page")!!.toInt()
        } catch (ignore: Exception) {
            1
        }
    }

    fun setSortType(sortType: SortType?) {
        this.sortType = sortType
        createUrl()
    }

    val tag: Tag
        get() {
            var t: Tag = tags.last()
            for (tt in tags) {
                if (tt.type !== TagType.LANGUAGE) return tt
                t = tt
            }
            return t
        }

    interface InspectorResponse {
        fun shouldStart(inspector: Inspector): Boolean
        fun onSuccess(galleries: List<GenericGallery>)
        fun onFailure(e: Exception)
        fun onStart()
        fun onEnd()
    }

    class InvalidResponseException : Exception()
    abstract class DefaultInspectorResponse : InspectorResponse {
        override fun shouldStart(inspector: Inspector): Boolean {
            return true
        }

        override fun onStart() {}
        override fun onEnd() {}
        override fun onSuccess(galleries: List<GenericGallery>) {}
        override fun onFailure(e: Exception) {
            e.localizedMessage?.let { error(it) }
        }
    }

    companion object {
        @JvmField
        val CREATOR: Creator<Inspector> = object : Creator<Inspector> {
            override fun createFromParcel(`in`: Parcel): Inspector {
                return Inspector(`in`)
            }

            override fun newArray(size: Int): Array<Inspector?> {
                return arrayOfNulls(size)
            }
        }

        fun favoriteInspector(
            context: Context,
            query: String?,
            page: Int,
            response: InspectorResponse?
        ): Inspector {
            val inspector = Inspector(context, response)
            inspector.page = page
            inspector.pageCount = 0
            inspector.query = query ?: ""
            inspector.requestType = ApiRequestType.FAVORITE
            inspector.tags = HashSet(1)
            inspector.createUrl()
            return inspector
        }

        fun randomInspector(
            context: Context,
            response: InspectorResponse,
            favorite: Boolean
        ): Inspector {
            val inspector = Inspector(context, response)
            inspector.requestType =
                if (favorite) ApiRequestType.RANDOM_FAVORITE else ApiRequestType.RANDOM
            inspector.createUrl()
            return inspector
        }

        fun galleryInspector(context: Context, id: Int, response: InspectorResponse): Inspector {
            val inspector = Inspector(context, response)
            inspector.id = id
            inspector.requestType = ApiRequestType.BYSINGLE
            inspector.createUrl()
            return inspector
        }

        fun basicInspector(context: Context, page: Int, response: InspectorResponse): Inspector {
            return searchInspector(context, null, null, page, sortType, null, response)
        }

        fun tagInspector(
            context: Context,
            tag: Tag,
            page: Int,
            sortType: SortType?,
            response: InspectorResponse?
        ): Inspector {
            val tags: Collection<Tag>
            if (!isOnlyTag) {
                tags = defaultTags
                tags.add(tag)
            } else {
                tags = setOf(tag)
            }
            return searchInspector(context, null, tags, page, sortType, null, response)
        }

        fun searchInspector(
            context: Context,
            query: String?,
            tags: Collection<Tag>?,
            page: Int,
            sortType: SortType?,
            ranges: Ranges?,
            response: InspectorResponse?
        ): Inspector {
            val inspector = Inspector(context, response)
            inspector.isCustom = tags != null
            inspector.tags = if (inspector.isCustom) HashSet(tags) else defaultTags
            inspector.tags.plus(getLanguageTags(getOnlyLanguage()))
            inspector.page = page
            inspector.pageCount = 0
            inspector.ranges = ranges
            inspector.query = query ?: ""
            inspector.sortType = sortType
            if (inspector.query.isEmpty() && (ranges == null || ranges.isDefault)) {
                when (inspector.tags.size) {
                    0 -> {
                        inspector.requestType = ApiRequestType.BYALL
                        inspector.tryByAllPopular()
                    }
                    1 -> {
                        inspector.requestType = ApiRequestType.BYTAG
                        if (inspector.tag.status !== TagStatus.AVOIDED)
                            inspector.requestType = ApiRequestType.BYSEARCH
                    }
                    else -> inspector.requestType = ApiRequestType.BYSEARCH
                }
            } else inspector.requestType = ApiRequestType.BYSEARCH
            inspector.createUrl()
            return inspector
        }

        private val defaultTags: HashSet<Tag>
            get() {
                val tags = HashSet(getAllStatus(TagStatus.ACCEPTED))
                tags.addAll(getLanguageTags(getOnlyLanguage()))
                if (removeAvoidedGalleries()) tags.addAll(getAllStatus(TagStatus.AVOIDED))
                tags.addAll(allOnlineBlacklisted)
                return tags
            }

        private fun getLanguageTags(onlyLanguage: Language?): Set<Tag> {
            val tags: MutableSet<Tag> = HashSet()
            if (onlyLanguage == null) return tags
            when (onlyLanguage) {
                Language.ENGLISH -> tags.add(getTagById(SpecialTagIds.LANGUAGE_ENGLISH.toInt())!!)
                Language.JAPANESE -> tags.add(getTagById(SpecialTagIds.LANGUAGE_JAPANESE.toInt())!!)
                Language.CHINESE -> tags.add(getTagById(SpecialTagIds.LANGUAGE_CHINESE.toInt())!!)
                else -> tags.add(getTagById(SpecialTagIds.LANGUAGE_ENGLISH.toInt())!!)
            }
            return tags
        }
    }
}
