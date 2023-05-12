package com.dublikunt.nclient.async.downloader

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.JobIntentService
import com.dublikunt.nclient.api.gallerys.Gallery
import com.dublikunt.nclient.api.gallerys.GenericGallery
import com.dublikunt.nclient.api.gallerys.SimpleGallery
import com.dublikunt.nclient.async.database.Queries.DownloadTable.getAllDownloads
import com.dublikunt.nclient.utility.LogUtility.download
import com.dublikunt.nclient.utility.LogUtility.error
import com.dublikunt.nclient.utility.Utility
import java.io.IOException

class DownloadGallery : JobIntentService() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val startCommand = super.onStartCommand(intent, flags, startId)
        if (intent != null) {
            val id = intent.getIntExtra("$packageName.ID", -1)
            val mode = intent.getStringExtra("$packageName.MODE")
            download("" + mode)
            val manager = DownloadQueue.managerFromId(id)
            if (manager != null) {
                download("IntentAction: $mode for id $id")
                assert(mode != null)
                when (mode) {
                    "STOP" -> DownloadQueue.remove(id, true)
                    "PAUSE" -> manager.downloader().state = GalleryDownloader.Status.PAUSED
                    "START" -> {
                        manager.downloader().state =
                            GalleryDownloader.Status.NOT_STARTED
                        DownloadQueue.givePriority(manager.downloader())
                        startWork(this)
                    }
                }
            }
        }
        return startCommand
    }

    override fun onHandleWork(intent: Intent) {
        while (true) {
            obtainData()
            val entry = DownloadQueue.fetch()
            if (entry == null) {
                synchronized(lock) {
                    try {
                        lock
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }
                continue
            }
            download("Downloading: " + entry.downloader().id)
            entry.downloader().download()
            Utility.threadSleep(1000)
        }
    }

    private fun obtainData() {
        var downloader = DownloadQueue.fetchForData()
        while (downloader != null) {
            downloader = DownloadQueue.fetchForData()
        }
    }

    companion object {
        private val lock = Any()
        private const val JOB_DOWNLOAD_GALLERY_ID = 9999
        fun downloadGallery(context: Context, gallery: GenericGallery) {
            if (gallery.valid && gallery is Gallery) downloadGallery(context, gallery)
            if (gallery.id > 0) {
                if (gallery is SimpleGallery) {
                    downloadGallery(context, gallery.title, gallery.getThumbnail(), gallery.id)
                } else downloadGallery(context, null, null, gallery.id)
            }
        }

        private fun downloadGallery(context: Context, title: String?, thumbnail: Uri?, id: Int) {
            if (id < 1) return
            DownloadQueue.add(GalleryDownloaderManager(context, title, thumbnail, id))
            startWork(context)
        }

        private fun downloadGallery(
            context: Context,
            gallery: Gallery,
            start: Int = 0,
            end: Int = gallery.pageCount - 1
        ) {
            DownloadQueue.add(GalleryDownloaderManager(context, gallery, start, end))
            startWork(context)
        }

        fun loadDownloads(context: Context) {
            try {
                val g = getAllDownloads(context)
                for (gg in g) {
                    gg.downloader().state = GalleryDownloader.Status.PAUSED
                    DownloadQueue.add(gg)
                }
                PageChecker().start()
                startWork(context)
            } catch (e: IOException) {
                error(e)
            }
        }

        fun downloadRange(context: Context, gallery: Gallery, start: Int, end: Int) {
            downloadGallery(context, gallery, start, end)
        }

        fun startWork(context: Context?) {
            if (context != null) enqueueWork(
                context,
                DownloadGallery::class.java,
                JOB_DOWNLOAD_GALLERY_ID,
                Intent()
            )
            synchronized(lock) { lock }
        }
    }
}
