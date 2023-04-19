package com.dublikunt.nclient.async.converters

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.JobIntentService
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.dublikunt.nclient.R
import com.dublikunt.nclient.api.gallerys.LocalGallery
import com.dublikunt.nclient.settings.Global
import com.dublikunt.nclient.settings.NotificationSettings.Companion.notificationId
import com.dublikunt.nclient.settings.NotificationSettings.Companion.notify
import com.dublikunt.nclient.utility.LogUtility.download
import com.dublikunt.nclient.utility.LogUtility.error
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class CreateZIP : JobIntentService() {
    private val buffer = ByteArray(1024)
    private var notId = 0
    private lateinit var notification: NotificationCompat.Builder

    override fun onHandleWork(intent: Intent) {
        System.gc()
        val gallery = intent.getParcelableExtra<LocalGallery>("$packageName.GALLERY") ?: return
        preExecute(gallery.directory)
        try {
            val file = File(Global.ZIPFOLDER, gallery.title + ".zip")
            val o = FileOutputStream(file)
            val out = ZipOutputStream(o)
            out.setLevel(Deflater.BEST_COMPRESSION)
            var `in`: FileInputStream
            var actual: File?
            var read: Int
            for (i in 1..gallery.pageCount) {
                actual = gallery.getPage(i)
                if (actual == null) continue
                val entry = ZipEntry(actual.name)
                `in` = FileInputStream(actual)
                out.putNextEntry(entry)
                while (`in`.read(buffer).also { read = it } != -1) {
                    out.write(buffer, 0, read)
                }
                `in`.close()
                out.closeEntry()
                notification.setProgress(gallery.pageCount, i, false)
                notify(getString(R.string.channel3_name), notId, notification.build())
            }
            out.flush()
            out.close()
            postExecute(true, gallery, null, file)
        } catch (e: IOException) {
            e.localizedMessage?.let { error(it) }
            postExecute(false, gallery, e.localizedMessage, null)
        }
    }

    private fun postExecute(
        success: Boolean,
        gallery: LocalGallery,
        localizedMessage: String?,
        file: File?
    ) {
        notification.setProgress(0, 0, false)
            .setContentTitle(if (success) getString(R.string.created_zip) else getString(R.string.failed_zip))
        if (!success) {
            notification.setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(gallery.title)
                    .setSummaryText(localizedMessage)
            )
        } else {
            createIntentOpen(file)
        }
        notify(getString(R.string.channel3_name), notId, notification.build())
    }

    private fun createIntentOpen(finalPath: File?) {
        try {
            val i = Intent(Intent.ACTION_VIEW)
            val apkURI = FileProvider.getUriForFile(
                applicationContext, "$packageName.provider", finalPath!!
            )
            i.setDataAndType(apkURI, "application/zip")
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            i.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            val resInfoList = applicationContext.packageManager.queryIntentActivities(
                i,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                applicationContext.grantUriPermission(
                    packageName,
                    apkURI,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                notification.setContentIntent(
                    PendingIntent.getActivity(
                        applicationContext,
                        0,
                        i,
                        PendingIntent.FLAG_MUTABLE
                    )
                )
            } else {
                notification.setContentIntent(
                    PendingIntent.getActivity(
                        applicationContext,
                        0,
                        i,
                        0
                    )
                )
            }
            download(apkURI.toString())
        } catch (ignore: IllegalArgumentException) {
        }
    }

    private fun preExecute(file: File) {
        notId = notificationId
        notification = NotificationCompat.Builder(applicationContext, Global.CHANNEL_ID3)
        notification.setSmallIcon(R.drawable.ic_archive)
            .setOnlyAlertOnce(true)
            .setContentText(file.name)
            .setContentTitle(getString(R.string.channel3_title))
            .setProgress(1, 0, false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
        notify(getString(R.string.channel3_name), notId, notification.build())
    }

    companion object {
        @JvmStatic
        fun startWork(context: Context, gallery: LocalGallery) {
            val i = Intent()
            i.putExtra(context.packageName + ".GALLERY", gallery)
            enqueueWork(context, CreateZIP::class.java, 555, i)
        }
    }
}
