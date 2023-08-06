package com.dublikunt.nclient.async.downloader;

public interface DownloadObserver {
    void triggerStartDownload(GalleryDownloader downloader);

    void triggerUpdateProgress(GalleryDownloader downloader, int reach, int total);

    void triggerEndDownload(GalleryDownloader downloader);

    void triggerCancelDownload(GalleryDownloader downloader);

    void triggerPauseDownload(GalleryDownloader downloader);
}
