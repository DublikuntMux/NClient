package com.dublikunt.nclient.async.downloader;

public class PageChecker extends Thread {
    @Override
    public void run() {
        for (GalleryDownloader g : DownloadQueue.getDownloaders())
            if (g.hasData()) g.initDownload();
    }
}
