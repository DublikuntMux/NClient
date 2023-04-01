package com.dublikunt.nclient.async.downloader

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.dublikunt.nclient.R
import com.dublikunt.nclient.api.Inspector
import com.dublikunt.nclient.api.LocalGallery
import com.dublikunt.nclient.api.components.Gallery
import com.dublikunt.nclient.async.database.Queries.DownloadTable.addGallery
import com.dublikunt.nclient.async.database.Queries.DownloadTable.removeGallery
import com.dublikunt.nclient.settings.Global
import com.dublikunt.nclient.settings.Global.client
import com.dublikunt.nclient.settings.Global.isJPEGCorrupted
import com.dublikunt.nclient.settings.Global.recursiveDelete
import com.dublikunt.nclient.utility.LogUtility.download
import com.dublikunt.nclient.utility.LogUtility.error
import com.dublikunt.nclient.utility.Utility
import okhttp3.Request
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.*
import java.util.concurrent.CopyOnWriteArraySet
import java.util.regex.Pattern
import kotlin.math.max

class GalleryDownloader(
    private val context: Context,
    title: String?,
    var thumbnail: Uri?,
    val id: Int
) {
    private val observers = CopyOnWriteArraySet<DownloadObserver>()
    private val urls: MutableList<PageContainer?> = ArrayList()
    var status = Status.NOT_STARTED
    var truePathTitle: String
        private set
    var start = -1
        private set
    var end = -1
        private set
    lateinit var gallery: Gallery
        private set
    var folder: File? = null
        private set
    private var initialized = false

    init {
        truePathTitle = Gallery.getPathTitle(title, context.getString(R.string.download_gallery))
    }

    constructor(context: Context, gallery: Gallery, start: Int, end: Int) : this(
        context,
        gallery.title,
        gallery.cover,
        gallery.id
    ) {
        this.start = start
        this.end = end
        setGallery(gallery)
    }

    fun hasData(): Boolean {
        return gallery != null
    }

    fun removeObserver(observer: DownloadObserver) {
        observers.remove(observer)
    }

    private fun setGallery(gallery: Gallery) {
        this.gallery = gallery
        truePathTitle = gallery.pathTitle
        thumbnail = gallery.thumbnail
        addGallery(this)
        if (start == -1) start = 0
        if (end == -1) end = gallery.pageCount - 1
    }

    private val totalPage: Int
        get() = max(1, end - start + 1)
    val percentage: Int
        get() = if (urls.size == 0) 0 else (totalPage - urls.size) * 100 / totalPage

    private fun onStart() {
        setStatus(Status.DOWNLOADING)
        for (observer in observers) observer.triggerStartDownload(this)
    }

    private fun onEnd() {
        setStatus(Status.FINISHED)
        for (observer in observers) observer.triggerEndDownload(this)
        download("Delete 75: $id")
        removeGallery(id)
    }

    private fun onUpdate() {
        val total = totalPage
        val reach = total - urls.size
        for (observer in observers) observer.triggerUpdateProgress(this, reach, total)
    }

    private fun onCancel() {
        for (observer in observers) observer.triggerCancelDownload(this)
    }

    private fun onPause() {
        for (observer in observers) observer.triggerPauseDownload(this)
    }

    fun localGallery(): LocalGallery? {
        return if (status != Status.FINISHED || folder == null) null else LocalGallery(folder!!)
    }

    fun addObserver(observer: DownloadObserver?) {
        if (observer == null) return
        observers.add(observer)
    }

    /**
     * @return true if the download has been completed, false otherwise
     */
    fun downloadGalleryData(): Boolean {
        if (gallery != null) return true
        val inspector = Inspector.galleryInspector(context, id, null)
        return try {
            inspector.createDocument()
            inspector.parseDocument()
            if (inspector.galleries == null || inspector.galleries.size == 0) return false
            val g = inspector.galleries[0] as Gallery
            if (g.isValid) setGallery(g)
            g.isValid
        } catch (e: Exception) {
            error("Error while downloading")
            false
        }
    }

    fun canBeFetched(): Boolean {
        return status != Status.FINISHED && status != Status.PAUSED
    }

    @JvmName("setStatusNormal")
    fun setStatus(status: Status) {
        if (this.status == status) return
        this.status = status
        if (status == Status.CANCELED) {
            download("Delete 95: $id")
            onCancel()
            recursiveDelete(folder)
            removeGallery(id)
        }
    }

    fun download() {
        initDownload()
        onStart()
        while (urls.isNotEmpty()) {
            downloadPage(urls[0])
            Utility.threadSleep(50)
            if (status == Status.PAUSED) {
                onPause()
                return
            }
            if (status == Status.CANCELED) {
                onCancel()
                return
            }
        }
        onEnd()
    }

    private fun downloadPage(page: PageContainer?) {
        if (savePage(page)) {
            urls.remove(page)
            onUpdate()
        }
    }

    private fun isCorrupted(file: File): Boolean {
        val path = file.absolutePath
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
            return isJPEGCorrupted(path)
        }
        val options = BitmapFactory.Options()
        options.inSampleSize = 256
        val bitmap = BitmapFactory.decodeFile(path, options)
        val x = bitmap == null
        if (!x) bitmap!!.recycle()
        return x
    }

    private fun savePage(page: PageContainer?): Boolean {
        if (page == null) return true
        val filePath = File(folder, page.pageName)
        download("Saving into: " + filePath + "," + page.url)
        if (filePath.exists() && !isCorrupted(filePath)) return true
        try {
            val r = client!!.newCall(Request.Builder().url(page.url).build()).execute()
            if (r.code != 200) {
                r.close()
                return false
            }
            val expectedSize = r.header("Content-Length", "-1")!!.toInt().toLong()
            val len = r.body.contentLength()
            if (len < 0 || expectedSize != len) {
                r.close()
                return false
            }
            val written = Utility.writeStreamToFile(r.body.byteStream(), filePath)
            r.close()
            if (written != len) {
                filePath.delete()
                return false
            }
            return true
        } catch (e: IOException) {
            error(e)
        } catch (e: NumberFormatException) {
            error(e)
        }
        return false
    }

    fun initDownload() {
        if (initialized) return
        initialized = true
        createFolder()
        createPages()
        checkPages()
    }

    private fun checkPages() {
        var filePath: File
        var i = 0
        while (i < urls.size) {
            if (urls[i] == null) {
                urls.removeAt(i--)
                i++
                continue
            }
            filePath = File(folder, urls[i]!!.pageName)
            if (filePath.exists() && !isCorrupted(filePath)) urls.removeAt(i--)
            i++
        }
    }

    private fun createPages() {
        var i = start
        while (i <= end && i < gallery.pageCount) {
            urls.add(
                PageContainer(
                    i + 1,
                    gallery.getHighPage(i).toString(),
                    gallery.getPageExtension(i)
                )
            )
            i++
        }
    }

    private fun createFolder() {
        folder = findFolder(Global.DOWNLOADFOLDER, truePathTitle, id)
        folder!!.mkdirs()
        try {
            writeNoMedia()
            createIdFile()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    private fun createIdFile() {
        val idFile = File(folder, ".$id")
        idFile.createNewFile()
    }

    @Throws(IOException::class)
    private fun writeNoMedia() {
        val nomedia = File(folder, ".nomedia")
        download("NOMEDIA: $nomedia for id $id")
        val writer = FileWriter(nomedia)
        gallery.jsonWrite(writer)
        writer.close()
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as GalleryDownloader
        return if (id != that.id) false else folder == that.folder
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + if (folder != null) folder.hashCode() else 0
        return result
    }

    enum class Status {
        NOT_STARTED, DOWNLOADING, PAUSED, FINISHED, CANCELED
    }

    class PageContainer(val page: Int, val url: String, val ext: String) {
        val pageName: String
            get() = String.format(Locale.US, "%03d.%s", page, ext)
    }

    companion object {
        private const val DUPLICATE_EXTENSION = ".DUP"
        private val ID_FILE: Pattern = Pattern.compile("^\\.\\d{1,6}$")
        private fun findFolder(downloadfolder: File?, pathTitle: String, id: Int): File {
            var folder = File(downloadfolder, pathTitle)
            if (usableFolder(folder, id)) return folder
            var i = 1
            do {
                folder = File(downloadfolder, pathTitle + DUPLICATE_EXTENSION + i++)
            } while (!usableFolder(folder, id))
            return folder
        }

        private fun usableFolder(file: File, id: Int): Boolean {
            if (!file.exists()) return true //folder not exists
            if (File(file, ".$id").exists()) return true //same id
            val files =
                file.listFiles { _: File?, name: String? -> ID_FILE.matcher(name).matches() }
            if (files != null && files.isNotEmpty()) return false //has id but not equal
            val localGallery = LocalGallery(file) //read id from metadata
            return localGallery.id == id
        }
    }
}
