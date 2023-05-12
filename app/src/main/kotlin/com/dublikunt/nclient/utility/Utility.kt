package com.dublikunt.nclient.utility

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.Menu
import androidx.core.content.FileProvider
import com.dublikunt.nclient.R
import com.dublikunt.nclient.settings.Global
import com.dublikunt.nclient.settings.Global.setTint
import com.dublikunt.nclient.utility.LogUtility.error
import java.io.*
import java.util.*

object Utility {
    val RANDOM = Random(System.nanoTime())
    const val ORIGINAL_URL = "nhentai.net"

    val baseUrl: String
        get() = "https://$host/"

    val host: String?
        get() = Global.mirror

    @Throws(IOException::class)
    private fun parseEscapedCharacter(reader: Reader, writer: Writer) {
        var toCreate: Int
        var read: Int
        when (reader.read().also { read = it }) {
            'u'.code -> {
                toCreate = 0
                var i = 0
                while (i < 4) {
                    toCreate *= 16
                    toCreate += Character.digit(reader.read(), 16)
                    i++
                }
                writer.write(toCreate)
            }

            'n'.code -> writer.write('\n'.code)
            't'.code -> writer.write('\t'.code)
            else -> {
                writer.write('\\'.code)
                writer.write(read)
            }
        }
    }

    fun unescapeUnicodeString(scriptHtml: String?): String {
        if (scriptHtml == null) return ""
        val reader = StringReader(scriptHtml)
        val writer = StringWriter()
        var actualChar: Int
        try {
            while (reader.read().also { actualChar = it } != -1) {
                if (actualChar != '\\'.code) writer.write(actualChar) else parseEscapedCharacter(
                    reader,
                    writer
                )
            }
        } catch (ignore: IOException) {
            return ""
        }
        return writer.toString()
    }

    fun threadSleep(millis: Long) {
        try {
            Thread.sleep(millis)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    fun tintMenu(menu: Menu) {
        val x = menu.size()
        for (i in 0 until x) {
            val item = menu.getItem(i)
            setTint(item.icon)
        }
    }

    private fun drawableToBitmap(dra: Drawable): Bitmap? {
        return if (dra !is BitmapDrawable) null else dra.bitmap
    }

    fun saveImage(drawable: Drawable, output: File) {
        val b = drawableToBitmap(drawable)
        if (b != null) saveImage(b, output)
    }

    private fun saveImage(bitmap: Bitmap, output: File) {
        try {
            if (!output.exists()) output.createNewFile()
            val ostream = FileOutputStream(output)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, ostream)
            ostream.flush()
            ostream.close()
        } catch (e: IOException) {
            e.localizedMessage?.let { error(it) }
        }
    }

    @Throws(IOException::class)
    fun writeStreamToFile(inputStream: InputStream, filePath: File?): Long {
        val outputStream = FileOutputStream(filePath)
        var read: Int
        var totalByte: Long = 0
        val bytes = ByteArray(1024)
        while (inputStream.read(bytes).also { read = it } != -1) {
            outputStream.write(bytes, 0, read)
            totalByte += read.toLong()
        }
        outputStream.flush()
        outputStream.close()
        inputStream.close()
        return totalByte
    }

    fun sendImage(context: Context, drawable: Drawable, text: String?) {
        var context = context
        context = context.applicationContext
        try {
            val tempFile = File.createTempFile("toSend", ".jpg")
            tempFile.deleteOnExit()
            val image = drawableToBitmap(drawable) ?: return
            saveImage(image, tempFile)
            var shareIntent = Intent(Intent.ACTION_SEND)
            if (text != null) shareIntent.putExtra(Intent.EXTRA_TEXT, text)
            val x = FileProvider.getUriForFile(context, context.packageName + ".provider", tempFile)
            shareIntent.putExtra(Intent.EXTRA_STREAM, x)
            shareIntent.type = "image/jpeg"
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val resInfoList = context.packageManager.queryIntentActivities(
                shareIntent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                context.grantUriPermission(
                    packageName,
                    x,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            shareIntent = Intent.createChooser(shareIntent, context.getString(R.string.share_with))
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(shareIntent)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
