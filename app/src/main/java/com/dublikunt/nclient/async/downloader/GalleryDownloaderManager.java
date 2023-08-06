package com.dublikunt.nclient.async.downloader;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.dublikunt.nclient.GalleryActivity;
import com.dublikunt.nclient.R;
import com.dublikunt.nclient.api.components.Gallery;
import com.dublikunt.nclient.settings.Global;
import com.dublikunt.nclient.settings.NotificationSettings;

import java.util.ConcurrentModificationException;
import java.util.Locale;

public class GalleryDownloaderManager {
    private final int notificationId = NotificationSettings.getNotificationId();
    private final GalleryDownloader downloaderV2;
    private final Context context;
    private NotificationCompat.Builder notification;
    private Gallery gallery;

    private final DownloadObserver observer = new DownloadObserver() {
        @Override
        public void triggerStartDownload(@NonNull GalleryDownloader downloader) {
            gallery = downloader.getGallery();
            prepareNotification();
            addActionToNotification(false);
            notificationUpdate();
        }

        @Override
        public void triggerUpdateProgress(GalleryDownloader downloader, int reach, int total) {
            setPercentage(reach, total);
            notificationUpdate();
        }

        @Override
        public void triggerEndDownload(GalleryDownloader downloader) {
            endNotification();
            addClickListener();
            notificationUpdate();
            DownloadQueue.remove(downloader, false);
        }

        @Override
        public void triggerCancelDownload(@NonNull GalleryDownloader downloader) {
            cancelNotification();
            Global.recursiveDelete(downloader.getFolder());
        }

        @Override
        public void triggerPauseDownload(GalleryDownloader downloader) {
            addActionToNotification(true);
            notificationUpdate();
        }
    };

    public GalleryDownloaderManager(Context context, Gallery gallery, int start, int end) {
        this.context = context;
        this.gallery = gallery;
        this.downloaderV2 = new GalleryDownloader(context, gallery, start, end);
        this.downloaderV2.addObserver(observer);
    }

    public GalleryDownloaderManager(Context context, String title, Uri thumbnail, int id) {
        this.context = context;
        this.downloaderV2 = new GalleryDownloader(context, title, thumbnail, id);
        this.downloaderV2.addObserver(observer);
    }

    private void cancelNotification() {
        NotificationSettings.cancel(context.getString(R.string.channel1_name), notificationId);
    }

    private void addClickListener() {
        Intent notifyIntent = new Intent(context, GalleryActivity.class);
        notifyIntent.putExtra(context.getPackageName() + ".GALLERY", downloaderV2.localGallery());
        notifyIntent.putExtra(context.getPackageName() + ".ISLOCAL", true);

        PendingIntent notifyPendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            notifyPendingIntent = PendingIntent.getActivity(
                context, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
            );
        } else {
            notifyPendingIntent = PendingIntent.getActivity(
                context, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT
            );
        }
        notification.setContentIntent(notifyPendingIntent);
    }

    public GalleryDownloader downloader() {
        return downloaderV2;
    }

    private void endNotification() {
        clearNotificationAction();
        hidePercentage();
        if (downloaderV2.getStatus() != GalleryDownloader.Status.CANCELED) {
            notification.setSmallIcon(R.drawable.ic_check);
            notification.setContentTitle(String.format(Locale.US, context.getString(R.string.completed_format), gallery.getTitle()));
        } else {
            notification.setSmallIcon(R.drawable.ic_close);
            notification.setContentTitle(String.format(Locale.US, context.getString(R.string.cancelled_format), gallery.getTitle()));
        }
    }

    private void hidePercentage() {
        setPercentage(0, 0);
    }

    private void setPercentage(int reach, int total) {
        notification.setProgress(total, reach, false);
    }

    private void prepareNotification() {
        notification = new NotificationCompat.Builder(context.getApplicationContext(), Global.channelID1);
        notification.setOnlyAlertOnce(true)

            .setContentTitle(String.format(Locale.US, context.getString(R.string.downloading_format), gallery.getTitle()))
            .setProgress(gallery.getPageCount(), 0, false)
            .setSmallIcon(R.drawable.ic_file_download);
        setPercentage(0, 1);
    }

    @SuppressLint("RestrictedApi")
    private void clearNotificationAction() {
        notification.mActions.clear();
    }

    private void addActionToNotification(boolean pauseMode) {
        clearNotificationAction();
        Intent startIntent = new Intent(context, DownloadGallery.class);
        Intent stopIntent = new Intent(context, DownloadGallery.class);
        Intent pauseIntent = new Intent(context, DownloadGallery.class);

        stopIntent.putExtra(context.getPackageName() + ".ID", downloaderV2.getId());
        pauseIntent.putExtra(context.getPackageName() + ".ID", downloaderV2.getId());
        startIntent.putExtra(context.getPackageName() + ".ID", downloaderV2.getId());

        stopIntent.putExtra(context.getPackageName() + ".MODE", "STOP");
        pauseIntent.putExtra(context.getPackageName() + ".MODE", "PAUSE");
        startIntent.putExtra(context.getPackageName() + ".MODE", "START");
        PendingIntent pStop;
        PendingIntent pPause;
        PendingIntent pStart;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            pStop = PendingIntent.getService(context, 0, stopIntent, PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_CANCEL_CURRENT);
            pPause = PendingIntent.getService(context, 1, pauseIntent, PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_CANCEL_CURRENT);
            pStart = PendingIntent.getService(context, 2, startIntent, PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_CANCEL_CURRENT);
        } else {
            pStop = PendingIntent.getService(context, 0, stopIntent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            pPause = PendingIntent.getService(context, 1, pauseIntent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            pStart = PendingIntent.getService(context, 2, startIntent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        }
        if (pauseMode)
            notification.addAction(R.drawable.ic_play_arrow, context.getString(R.string.resume), pStart);
        else notification.addAction(R.drawable.ic_pause, context.getString(R.string.pause), pPause);
        notification.addAction(R.drawable.ic_close, context.getString(R.string.cancel), pStop);
    }


    private synchronized void notificationUpdate() {
        try {
            NotificationSettings.notify(context.getString(R.string.channel1_name), notificationId, notification.build());
        } catch (NullPointerException | ConcurrentModificationException ignore) {
        }
    }

}
