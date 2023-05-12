package com.dublikunt.nclient.async.downloader

import java.util.concurrent.CopyOnWriteArrayList

object DownloadQueue {
    private val downloadQueue: MutableList<GalleryDownloaderManager> = CopyOnWriteArrayList()
    fun add(x: GalleryDownloaderManager) {
        for (manager in downloadQueue) if (x.downloader().id == manager.downloader().id) {
            manager.downloader().state = GalleryDownloader.Status.NOT_STARTED
            givePriority(manager.downloader())
            return
        }
        downloadQueue.add(x)
    }

    fun fetchForData(): GalleryDownloader? {
        for (x in downloadQueue) if (!x.downloader().hasData()) return x.downloader()
        return null
    }

    fun fetch(): GalleryDownloaderManager? {
        for (x in downloadQueue) if (x.downloader().canBeFetched()) return x
        return null
    }

    fun clear() {
        for (x in downloadQueue) x.downloader().state = GalleryDownloader.Status.CANCELED
        downloadQueue.clear()
    }

    val downloaders: CopyOnWriteArrayList<GalleryDownloader>
        get() {
            val downloaders = CopyOnWriteArrayList<GalleryDownloader>()
            for (manager in downloadQueue) downloaders.add(manager.downloader())
            return downloaders
        }

    fun addObserver(observer: DownloadObserver?) {
        for (manager in downloadQueue) manager.downloader().addObserver(observer)
    }

    fun removeObserver(observer: DownloadObserver) {
        for (manager in downloadQueue) manager.downloader().removeObserver(observer)
    }

    private fun findManagerFromDownloader(downloader: GalleryDownloader?): GalleryDownloaderManager? {
        for (manager in downloadQueue) if (manager.downloader() === downloader) return manager
        return null
    }

    fun remove(id: Int, cancel: Boolean) {
        remove(findDownloaderFromId(id), cancel)
    }

    private fun findDownloaderFromId(id: Int): GalleryDownloader? {
        for (manager in downloadQueue) if (manager.downloader().id == id) return manager.downloader()
        return null
    }

    fun remove(downloader: GalleryDownloader?, cancel: Boolean) {
        val manager = findManagerFromDownloader(downloader) ?: return
        if (cancel) downloader!!.state = GalleryDownloader.Status.CANCELED
        downloadQueue.remove(manager)
    }

    fun givePriority(downloader: GalleryDownloader?) {
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
