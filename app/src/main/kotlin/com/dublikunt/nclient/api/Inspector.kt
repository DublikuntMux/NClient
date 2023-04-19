package com.dublikunt.nclient.api

import android.content.Context
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import com.dublikunt.nclient.api.comments.Ranges
import com.dublikunt.nclient.api.comments.Tag
import com.dublikunt.nclient.api.gallerys.Gallery
import com.dublikunt.nclient.api.gallerys.GenericGallery
import com.dublikunt.nclient.api.gallerys.LocalGallery
import com.dublikunt.nclient.api.gallerys.SimpleGallery
import com.dublikunt.nclient.async.database.Queries.TagTable.allOnlineBlacklisted
import com.dublikunt.nclient.async.database.Queries.TagTable.getAllStatus
import com.dublikunt.nclient.async.database.Queries.TagTable.getTagById
import com.dublikunt.nclient.enums.ApiRequestType
import com.dublikunt.nclient.enums.Language
import com.dublikunt.nclient.enums.SortType
import com.dublikunt.nclient.enums.SpecialTagIds
import com.dublikunt.nclient.enums.TagStatus
import com.dublikunt.nclient.enums.TagType
import com.dublikunt.nclient.settings.Global.client
import com.dublikunt.nclient.settings.Global.getOnlyLanguage
import com.dublikunt.nclient.settings.Global.isOnlyTag
import com.dublikunt.nclient.settings.Global.removeAvoidedGalleries
import com.dublikunt.nclient.settings.Global.sortType
import com.dublikunt.nclient.utility.LogUtility
import com.dublikunt.nclient.utility.Utility.baseUrl
import com.dublikunt.nclient.utility.Utility.unescapeUnicodeString
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URLEncoder

open class Inspector : Thread, Parcelable {
    private lateinit var sortType: SortType
    var isCustom = false
        private set
    private var forceStart = false
    var page = 0
        set(value) {
            field = value
            createUrl()
        }
    var pageCount = -1
        private set
    private var id = 0
    var query: String? = null
        private set
    lateinit var url: String
        private set
    var requestType: ApiRequestType? = null
        private set
    private lateinit var tags: HashSet<Tag>
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
        query = `in`.readString()
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
        ranges = `in`.readParcelable(Ranges::class.java.classLoader)!!
    }

    private constructor(context: Context, response: InspectorResponse?) {
        initialize(context, response)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        if (sortType != null) dest.writeByte(sortType.ordinal.toByte()) else dest.writeByte(
            SortType.RECENT_ALL_TIME.ordinal.toByte()
        )
        dest.writeByte((if (isCustom) 1 else 0).toByte())
        dest.writeInt(page)
        dest.writeInt(pageCount)
        dest.writeInt(id)
        dest.writeString(query)
        dest.writeString(url)
        dest.writeByte(requestType!!.ordinal())
        if (galleries == null || galleries.size == 0) dest.writeByte(GenericGallery.Type.SIMPLE.ordinal.toByte()) else dest.writeByte(
            galleries[0].type.ordinal.toByte()
        )
        dest.writeTypedList(galleries)
        dest.writeTypedList(ArrayList(tags))
        dest.writeParcelable(ranges, flags)
    }

    val searchTitle: String?
        get() =
            if (query!!.isNotEmpty()) query else url.replace(baseUrl + "search/?q=", "")
                .replace('+', ' ')

    fun initialize(context: Context, response: InspectorResponse?) {
        this.response = response
        this.context = WeakReference(context)
    }

    fun cloneInspector(context: Context, response: InspectorResponse): Inspector {
        val inspectorV3 = Inspector(context, response)
        inspectorV3.query = query
        inspectorV3.url = url
        inspectorV3.tags = tags
        inspectorV3.requestType = requestType
        inspectorV3.sortType = sortType
        inspectorV3.pageCount = pageCount
        inspectorV3.page = page
        inspectorV3.id = id
        inspectorV3.isCustom = isCustom
        inspectorV3.ranges = ranges
        return inspectorV3
    }

    private fun tryByAllPopular() {
        if (sortType !== SortType.RECENT_ALL_TIME) {
            requestType = ApiRequestType.BYSEARCH
            query = "-nclientv2"
        }
    }

    private fun createUrl() {
        val query: String?
        query = try {
            if (this.query == null) null else URLEncoder.encode(this.query, "UTF-8")
        } catch (ignore: UnsupportedEncodingException) {
            this.query
        }
        val builder = StringBuilder(baseUrl)
        if (requestType === ApiRequestType.BYALL) builder.append("?page=")
            .append(page) else if (requestType === ApiRequestType.RANDOM) builder.append("random/") else if (requestType === ApiRequestType.RANDOM_FAVORITE) builder.append(
            "favorites/random"
        ) else if (requestType === ApiRequestType.BYSINGLE) builder.append("g/")
            .append(id) else if (requestType === ApiRequestType.FAVORITE) {
            builder.append("favorites/")
            if (query != null && query.length > 0) builder.append("?q=").append(query)
                .append('&') else builder.append('?')
            builder.append("page=").append(page)
        } else if (requestType === ApiRequestType.BYSEARCH || requestType === ApiRequestType.BYTAG) {
            builder.append("search/?q=").append(query)
            for (tt in tags) {
                if (builder.toString().contains(tt.toQueryTag(TagStatus.ACCEPTED))) continue
                builder.append('+').append(URLEncoder.encode(tt.toQueryTag()))
            }
            if (ranges != null) builder.append('+').append(ranges!!.toQuery())
            builder.append("&page=").append(page)
            if (sortType.urlAddition != null) {
                builder.append("&sort=").append(sortType.urlAddition)
            }
        }
        url = builder.toString().replace(' ', '+')
        LogUtility.download("WWW: $bookmarkURL")
    }

    fun forceStart() {
        forceStart = true
        start()
    }

    fun setSortType(sortType: SortType) {
        this.sortType = sortType
        createUrl()
    }

    private val bookmarkURL: String
        get() = if (page < 2) url else url.substring(0, url.lastIndexOf('=') + 1)

    @Throws(IOException::class)
    fun createDocument(): Boolean {
        if (htmlDocument != null) return true
        val response = client!!.newCall(Request.Builder().url(url).build()).execute()
        this.htmlDocument = (Jsoup.parse(response.body.byteStream(), "UTF-8", baseUrl))
        response.close()
        return response.code == HttpURLConnection.HTTP_OK
    }

    @Throws(IOException::class, InvalidResponseException::class)
    fun parseDocument() {
        if (requestType!!.isSingle) doSingle(htmlDocument!!.body()) else doSearch(htmlDocument!!.body())
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
        LogUtility.download("Starting download: $url")
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
        LogUtility.download("Finished download: $url")
    }

    private fun filterDocumentTags() {
        val galleryTag = ArrayList<SimpleGallery>(
            galleries.size
        )
        for (gal in galleries) {
            assert(gal is SimpleGallery)
            val gallery = gal as SimpleGallery
            if (gallery.hasTags(tags)) {
                galleryTag.add(gallery)
            }
        }
        galleries.clear()
        galleries.addAll(galleryTag)
    }

    @Throws(IOException::class, InvalidResponseException::class)
    private fun doSingle(document: Element) {
        galleries = ArrayList(1)
        val scripts = document.getElementsByTag("script")
        if (scripts.isEmpty()) throw InvalidResponseException()
        val json = trimScriptTag(scripts.last()!!.html()) ?: throw InvalidResponseException()
        val relContainer = document.getElementById("related-container")
        val rel: Elements
        rel = if (relContainer != null) relContainer.getElementsByClass("gallery") else Elements()
        val isFavorite: Boolean
        isFavorite = try {
            document.getElementById("favorite")!!.getElementsByTag("span")[0].text() == "Unfavorite"
        } catch (e: Exception) {
            false
        }
        LogUtility.download("is favorite? $isFavorite")
        galleries.add(Gallery(context!!.get()!!, json, rel, isFavorite))
    }

    private fun trimScriptTag(scriptHtml: String): String? {
        var scriptHtml = scriptHtml
        var s = scriptHtml.indexOf("parse")
        if (s < 0) return null
        s += 7
        scriptHtml = scriptHtml.substring(s, scriptHtml.lastIndexOf(");") - 1)
        scriptHtml = unescapeUnicodeString(scriptHtml)
        return if (scriptHtml.isEmpty()) null else scriptHtml
    }

    @Throws(InvalidResponseException::class)
    private fun doSearch(document: Element) {
        var gal = document.getElementsByClass("gallery")
        galleries = ArrayList(gal.size)
        for (e in gal) galleries.add(SimpleGallery(context!!.get()!!, e))
        gal = document.getElementsByClass("last")
        pageCount = if (gal.size == 0) Math.max(1, page) else findTotal(gal.last())
        if (document.getElementById("content") == null) throw InvalidResponseException()
    }

    private fun findTotal(e: Element?): Int {
        val temp = e!!.attr("href")
        return try {
            Uri.parse(temp).getQueryParameter("page")!!.toInt()
        } catch (ignore: Exception) {
            1
        }
    }

    val tag: Tag
        get() {
            var t: Tag = tags.first()
            for (tt in tags) {
                if (tt.type !== TagType.LANGUAGE) return tt
                t = tt
            }
            return t
        }

    class InvalidResponseException : Exception()
    interface InspectorResponse {
        fun shouldStart(inspector: Inspector): Boolean
        fun onSuccess(galleries: List<GenericGallery>)
        fun onFailure(e: Exception)
        fun onStart()
        fun onEnd()
    }

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
            sortType: SortType,
            response: InspectorResponse
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
            sortType: SortType,
            ranges: Ranges?,
            response: InspectorResponse?
        ): Inspector {
            val inspector = Inspector(context, response)
            inspector.isCustom = tags != null
            inspector.tags = if (inspector.isCustom) HashSet(tags) else defaultTags
            (inspector.tags).addAll(getLanguageTags(getOnlyLanguage()))
            inspector.page = page
            inspector.pageCount = 0
            inspector.ranges = ranges
            inspector.query = query ?: ""
            inspector.sortType = sortType
            if (inspector.query!!.isEmpty() && (ranges == null || ranges.isDefault)) {
                when ((inspector.tags).size) {
                    0 -> {
                        inspector.requestType = ApiRequestType.BYALL
                        inspector.tryByAllPopular()
                    }

                    1 -> {
                        inspector.requestType = ApiRequestType.BYTAG
                        //else by search for the negative tag
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
            val tags: HashSet<Tag> = HashSet()
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
