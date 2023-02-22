package com.dublikunt.nclient.async.downloader

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.dublikunt.nclient.GalleryActivity
import com.dublikunt.nclient.R
import com.dublikunt.nclient.api.components.Gallery
import com.dublikunt.nclient.settings.Global
import com.dublikunt.nclient.settings.Global.recursiveDelete
import com.dublikunt.nclient.settings.NotificationSettings
import com.dublikunt.nclient.settings.NotificationSettings.Companion.cancel
import com.dublikunt.nclient.settings.NotificationSettings.Companion.notify
import java.util.*

class GalleryDownloaderManager {
    private val notificationId = NotificationSettings.notificationId
    private val downloaderV2: GalleryDownloader
    private val context: Context
    private var notification: NotificationCompat.Builder? = null
    private var gallery: Gallery? = null
    private val observer: DownloadObserver = object : DownloadObserver {
        override fun triggerStartDownload(downloader: GalleryDownloader) {
            gallery = downloader.gallery
            prepareNotification()
            addActionToNotification(false)
            notificationUpdate()
        }

        override fun triggerUpdateProgress(
            downloader: GalleryDownloader?,
            reach: Int,
            total: Int
        ) {
            setPercentage(reach, total)
            notificationUpdate()
        }

        override fun triggerEndDownload(downloader: GalleryDownloader?) {
            endNotification()
            addClickListener()
            notificationUpdate()
            DownloadQueue.remove(downloader, false)
        }

        override fun triggerCancelDownload(downloader: GalleryDownloader) {
            cancelNotification()
            recursiveDelete(downloader.folder)
        }

        override fun triggerPauseDownload(downloader: GalleryDownloader?) {
            addActionToNotification(true)
            notificationUpdate()
        }
    }

    constructor(context: Context, gallery: Gallery, start: Int, end: Int) {
        this.context = context
        this.gallery = gallery
        downloaderV2 = GalleryDownloader(context, gallery, start, end)
        downloaderV2.addObserver(observer)
    }

    constructor(context: Context, title: String?, thumbnail: Uri?, id: Int) {
        this.context = context
        downloaderV2 = GalleryDownloader(context, title, thumbnail, id)
        downloaderV2.addObserver(observer)
    }

    private fun cancelNotification() {
        cancel(context.getString(R.string.channel1_name), notificationId)
    }

    private fun addClickListener() {
        val notifyIntent = Intent(context, GalleryActivity::class.java)
        notifyIntent.putExtra(context.packageName + ".GALLERY", downloaderV2.localGallery())
        notifyIntent.putExtra(context.packageName + ".ISLOCAL", true)
        // Create the PendingIntent
        val notifyPendingIntent: PendingIntent =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.getActivity(
                    context,
                    0,
                    notifyIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
            } else {
                PendingIntent.getActivity(
                    context, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT
                )
            }
        notification!!.setContentIntent(notifyPendingIntent)
    }

    fun downloader(): GalleryDownloader {
        return downloaderV2
    }

    private fun endNotification() {
        clearNotificationAction()
        hidePercentage()
        if (downloaderV2.status != GalleryDownloader.Status.CANCELED) {
            notification!!.setSmallIcon(R.drawable.ic_check)
            notification!!.setContentTitle(
                String.format(
                    Locale.US,
                    context.getString(R.string.completed_format),
                    gallery!!.title
                )
            )
        } else {
            notification!!.setSmallIcon(R.drawable.ic_close)
            notification!!.setContentTitle(
                String.format(
                    Locale.US,
                    context.getString(R.string.cancelled_format),
                    gallery!!.title
                )
            )
        }
    }

    private fun hidePercentage() {
        setPercentage(0, 0)
    }

    private fun setPercentage(reach: Int, total: Int) {
        notification!!.setProgress(total, reach, false)
    }

    private fun prepareNotification() {
        notification = NotificationCompat.Builder(context.applicationContext, Global.CHANNEL_ID1)
        notification!!.setOnlyAlertOnce(true)
            .setContentTitle(
                String.format(
                    Locale.US,
                    context.getString(R.string.downloading_format),
                    gallery!!.title
                )
            )
            .setProgress(gallery!!.pageCount, 0, false)
            .setSmallIcon(R.drawable.ic_file_download)
        setPercentage(0, 1)
    }

    @SuppressLint("RestrictedApi")
    private fun clearNotificationAction() {
        notification!!.mActions.clear()
    }

    private fun addActionToNotification(pauseMode: Boolean) {
        clearNotificationAction()
        val startIntent = Intent(context, DownloadGallery::class.java)
        val stopIntent = Intent(context, DownloadGallery::class.java)
        val pauseIntent = Intent(context, DownloadGallery::class.java)
        stopIntent.putExtra(context.packageName + ".ID", downloaderV2.id)
        pauseIntent.putExtra(context.packageName + ".ID", downloaderV2.id)
        startIntent.putExtra(context.packageName + ".ID", downloaderV2.id)
        stopIntent.putExtra(context.packageName + ".MODE", "STOP")
        pauseIntent.putExtra(context.packageName + ".MODE", "PAUSE")
        startIntent.putExtra(context.packageName + ".MODE", "START")
        val pStop: PendingIntent
        val pPause: PendingIntent
        val pStart: PendingIntent
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pStop = PendingIntent.getService(
                context,
                0,
                stopIntent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
            )
            pPause = PendingIntent.getService(
                context,
                1,
                pauseIntent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
            )
            pStart = PendingIntent.getService(
                context,
                2,
                startIntent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
            )
        } else {
            pStop =
                PendingIntent.getService(context, 0, stopIntent, PendingIntent.FLAG_CANCEL_CURRENT)
            pPause =
                PendingIntent.getService(context, 1, pauseIntent, PendingIntent.FLAG_CANCEL_CURRENT)
            pStart =
                PendingIntent.getService(context, 2, startIntent, PendingIntent.FLAG_CANCEL_CURRENT)
        }
        if (pauseMode) notification!!.addAction(
            R.drawable.ic_play_arrow,
            context.getString(R.string.resume),
            pStart
        ) else notification!!.addAction(
            R.drawable.ic_pause,
            context.getString(R.string.pause),
            pPause
        )
        notification!!.addAction(R.drawable.ic_close, context.getString(R.string.cancel), pStop)
    }

    @Synchronized
    private fun notificationUpdate() {
        try {
            notify(
                context.getString(R.string.channel1_name),
                notificationId,
                notification!!.build()
            )
        } catch (ignore: NullPointerException) {
        } catch (ignore: ConcurrentModificationException) {
        }
    }
}
