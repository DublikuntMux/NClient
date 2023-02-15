package com.dublikunt.nclient.async.downloader

import java.util.concurrent.CopyOnWriteArrayList

object DownloadQueue {
    private val downloadQueue: MutableList<GalleryDownloaderManager> = CopyOnWriteArrayList()
    fun add(x: GalleryDownloaderManager) {
        for (manager in downloadQueue) if (x.downloader().id == manager.downloader().id) {
            manager.downloader().status = GalleryDownloaderV2.Status.NOT_STARTED
            givePriority(manager.downloader())
            return
        }
        downloadQueue.add(x)
    }

    fun fetchForData(): GalleryDownloaderV2? {
        for (x in downloadQueue) if (!x.downloader().hasData()) return x.downloader()
        return null
    }

    fun fetch(): GalleryDownloaderManager? {
        for (x in downloadQueue) if (x.downloader().canBeFetched()) return x
        return null
    }

    fun clear() {
        for (x in downloadQueue) x.downloader().status = GalleryDownloaderV2.Status.CANCELED
        downloadQueue.clear()
    }

    @JvmStatic
    val downloaders: CopyOnWriteArrayList<GalleryDownloaderV2?>
        get() {
            val downloaders = CopyOnWriteArrayList<GalleryDownloaderV2?>()
            for (manager in downloadQueue) downloaders.add(manager.downloader())
            return downloaders
        }

    @JvmStatic
    fun addObserver(observer: DownloadObserver?) {
        for (manager in downloadQueue) manager.downloader().addObserver(observer)
    }

    @JvmStatic
    fun removeObserver(observer: DownloadObserver) {
        for (manager in downloadQueue) manager.downloader().removeObserver(observer)
    }

    private fun findManagerFromDownloader(downloader: GalleryDownloaderV2?): GalleryDownloaderManager? {
        for (manager in downloadQueue) if (manager.downloader() === downloader) return manager
        return null
    }

    fun remove(id: Int, cancel: Boolean) {
        remove(findDownloaderFromId(id), cancel)
    }

    private fun findDownloaderFromId(id: Int): GalleryDownloaderV2? {
        for (manager in downloadQueue) if (manager.downloader().id == id) return manager.downloader()
        return null
    }

    @JvmStatic
    fun remove(downloader: GalleryDownloaderV2?, cancel: Boolean) {
        val manager = findManagerFromDownloader(downloader) ?: return
        if (cancel) downloader!!.status = GalleryDownloaderV2.Status.CANCELED
        downloadQueue.remove(manager)
    }

    @JvmStatic
    fun givePriority(downloader: GalleryDownloaderV2?) {
        val manager = findManagerFromDownloader(downloader) ?: return
        downloadQueue.remove(manager)
        downloadQueue.add(0, manager)
    }

    fun managerFromId(id: Int): GalleryDownloaderManager? {
        for (manager in downloadQueue) if (manager.downloader().id == id) return manager
        return null
    }

    val isEmpty: Boolean
        get() = downloadQueue.size == 0
}
