package com.dublikunt.nclient.async.downloader

interface DownloadObserver {
    fun triggerStartDownload(downloader: GalleryDownloaderV2)
    fun triggerUpdateProgress(downloader: GalleryDownloaderV2?, reach: Int, total: Int)
    fun triggerEndDownload(downloader: GalleryDownloaderV2?)
    fun triggerCancelDownload(downloader: GalleryDownloaderV2)
    fun triggerPauseDownload(downloader: GalleryDownloaderV2?)
}
