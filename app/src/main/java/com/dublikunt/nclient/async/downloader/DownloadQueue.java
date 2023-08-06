package com.dublikunt.nclient.async.downloader;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DownloadQueue {
    private static final List<GalleryDownloaderManager> downloadQueue = new CopyOnWriteArrayList<>();

    public static void add(GalleryDownloaderManager x) {
        for (GalleryDownloaderManager manager : downloadQueue)
            if (x.downloader().getId() == manager.downloader().getId()) {
                manager.downloader().setStatus(GalleryDownloader.Status.NOT_STARTED);
                givePriority(manager.downloader());
                return;
            }
        downloadQueue.add(x);
    }

    public static GalleryDownloader fetchForData() {
        for (GalleryDownloaderManager x : downloadQueue)
            if (!x.downloader().hasData()) return x.downloader();
        return null;
    }

    public static GalleryDownloaderManager fetch() {
        for (GalleryDownloaderManager x : downloadQueue)
            if (x.downloader().canBeFetched()) return x;
        return null;
    }

    public static void clear() {
        for (GalleryDownloaderManager x : downloadQueue)
            x.downloader().setStatus(GalleryDownloader.Status.CANCELED);
        downloadQueue.clear();
    }

    public static CopyOnWriteArrayList<GalleryDownloader> getDownloaders() {
        CopyOnWriteArrayList<GalleryDownloader> downloaders = new CopyOnWriteArrayList<>();
        for (GalleryDownloaderManager manager : downloadQueue)
            downloaders.add(manager.downloader());
        return downloaders;
    }

    public static void addObserver(DownloadObserver observer) {
        for (GalleryDownloaderManager manager : downloadQueue)
            manager.downloader().addObserver(observer);
    }

    public static void removeObserver(DownloadObserver observer) {
        for (GalleryDownloaderManager manager : downloadQueue)
            manager.downloader().removeObserver(observer);
    }

    private static GalleryDownloaderManager findManagerFromDownloader(GalleryDownloader downloader) {
        for (GalleryDownloaderManager manager : downloadQueue)
            if (manager.downloader() == downloader)
                return manager;
        return null;
    }

    public static void remove(int id, boolean cancel) {
        remove(findDownloaderFromId(id), cancel);
    }

    private static GalleryDownloader findDownloaderFromId(int id) {
        for (GalleryDownloaderManager manager : downloadQueue)
            if (manager.downloader().getId() == id) return manager.downloader();
        return null;
    }

    public static void remove(GalleryDownloader downloader, boolean cancel) {
        GalleryDownloaderManager manager = findManagerFromDownloader(downloader);
        if (manager == null) return;
        if (cancel)
            downloader.setStatus(GalleryDownloader.Status.CANCELED);
        downloadQueue.remove(manager);
    }

    public static void givePriority(GalleryDownloader downloader) {
        GalleryDownloaderManager manager = findManagerFromDownloader(downloader);
        if (manager == null) return;
        downloadQueue.remove(manager);
        downloadQueue.add(0, manager);
    }

    public static GalleryDownloaderManager managerFromId(int id) {
        for (GalleryDownloaderManager manager : downloadQueue)
            if (manager.downloader().getId() == id) return manager;
        return null;
    }

    public static boolean isEmpty() {
        return downloadQueue.size() == 0;

    }
}
