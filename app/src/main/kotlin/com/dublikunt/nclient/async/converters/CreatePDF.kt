package com.dublikunt.nclient.async.converters

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfDocument.PageInfo
import android.os.Build
import androidx.core.app.JobIntentService
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.dublikunt.nclient.R
import com.dublikunt.nclient.api.LocalGallery
import com.dublikunt.nclient.settings.Global
import com.dublikunt.nclient.settings.NotificationSettings.Companion.notificationId
import com.dublikunt.nclient.settings.NotificationSettings.Companion.notify
import com.dublikunt.nclient.utility.LogUtility.download
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class CreatePDF : JobIntentService() {
    private var notId = 0
    private var totalPage = 0
    private var notification: NotificationCompat.Builder? = null
    override fun onHandleWork(intent: Intent) {
        if (!hasPDFCapabilities()) {
            return
        }
        notId = notificationId
        System.gc()
        val gallery = intent.getParcelableExtra<LocalGallery>("$packageName.GALLERY") ?: return
        totalPage = gallery.pageCount
        preExecute(gallery.directory)
        val document = PdfDocument()
        var page: File?
        for (a in 1..gallery.pageCount) {
            page = gallery.getPage(a)
            if (page == null) continue
            val bitmap = BitmapFactory.decodeFile(page.absolutePath)
            if (bitmap != null) {
                val info = PageInfo.Builder(bitmap.width, bitmap.height, a).create()
                val p = document.startPage(info)
                p.canvas.drawBitmap(bitmap, 0f, 0f, null)
                document.finishPage(p)
                bitmap.recycle()
            }
            notification!!.setProgress(totalPage - 1, a + 1, false)
            notify(getString(R.string.channel2_name), notId, notification!!.build())
        }
        notification!!.setContentText(getString(R.string.writing_pdf))
        notification!!.setProgress(totalPage, 0, true)
        notify(getString(R.string.channel2_name), notId, notification!!.build())
        try {
            var finalPath = Global.PDFFOLDER
            finalPath!!.mkdirs()
            finalPath = File(finalPath, gallery.title + ".pdf")
            finalPath.createNewFile()
            download("Generating PDF at: $finalPath")
            val out = FileOutputStream(finalPath)
            document.writeTo(out)
            out.close()
            document.close()
            notification!!.setProgress(0, 0, false)
            notification!!.setContentTitle(getString(R.string.created_pdf))
            notification!!.setContentText(gallery.title)
            createIntentOpen(finalPath)
            notify(getString(R.string.channel2_name), notId, notification!!.build())
            download(finalPath.absolutePath)
        } catch (e: IOException) {
            notification!!.setContentTitle(getString(R.string.error_pdf))
            notification!!.setContentText(getString(R.string.failed))
            notification!!.setProgress(0, 0, false)
            notify(getString(R.string.channel2_name), notId, notification!!.build())
            throw RuntimeException("Error generating file", e)
        } finally {
            document.close()
        }
    }

    private fun createIntentOpen(finalPath: File) {
        try {
            val i = Intent(Intent.ACTION_VIEW)
            val apkURI = FileProvider.getUriForFile(
                applicationContext, "$packageName.provider", finalPath
            )
            i.setDataAndType(apkURI, "application/pdf")
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
                notification!!.setContentIntent(
                    PendingIntent.getActivity(
                        applicationContext,
                        0,
                        i,
                        PendingIntent.FLAG_MUTABLE
                    )
                )
            } else {
                notification!!.setContentIntent(
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
        notification = NotificationCompat.Builder(applicationContext, Global.CHANNEL_ID2)
        notification!!.setSmallIcon(R.drawable.ic_pdf)
            .setOnlyAlertOnce(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(file.name))
            .setContentTitle(getString(R.string.channel2_title))
            .setContentText(getString(R.string.parsing_pages))
            .setProgress(totalPage - 1, 0, false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
        notify(getString(R.string.channel2_name), notId, notification!!.build())
    }

    companion object {
        @JvmStatic
        fun startWork(context: Context, gallery: LocalGallery) {
            val i = Intent()
            i.putExtra(context.packageName + ".GALLERY", gallery)
            enqueueWork(context, CreatePDF::class.java, 444, i)
        }

        fun hasPDFCapabilities(): Boolean {
            return try {
                Class.forName("android.graphics.pdf.PdfDocument")
                true
            } catch (e: ClassNotFoundException) {
                false
            }
        }
    }
}
