package com.dublikunt.nclient.async.downloader

interface DownloadObserver {
    fun triggerStartDownload(downloader: GalleryDownloader)
    fun triggerUpdateProgress(downloader: GalleryDownloader?, reach: Int, total: Int)
    fun triggerEndDownload(downloader: GalleryDownloader?)
    fun triggerCancelDownload(downloader: GalleryDownloader)
    fun triggerPauseDownload(downloader: GalleryDownloader?)
}
