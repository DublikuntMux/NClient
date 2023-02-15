package com.dublikunt.nclient.async.database.export

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.text.format.DateFormat
import android.util.JsonWriter
import com.dublikunt.nclient.SettingsActivity
import com.dublikunt.nclient.async.database.Queries
import com.dublikunt.nclient.settings.Database.database
import com.dublikunt.nclient.utility.LogUtility.error
import java.io.IOException
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.util.*
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object Exporter {
    const val DB_ZIP_FILE = "Database.json"
    private val SHARED_FILES = arrayOf(
        "Settings",
        "ScrapedTags"
    )
    private val SCHEMAS = arrayOf(
        Queries.GalleryTable.TABLE_NAME,
        Queries.TagTable.TABLE_NAME,
        Queries.GalleryBridgeTable.TABLE_NAME,
        Queries.BookmarkTable.TABLE_NAME,
        Queries.DownloadTable.TABLE_NAME,
        Queries.HistoryTable.TABLE_NAME,
        Queries.FavoriteTable.TABLE_NAME,
        Queries.ResumeTable.TABLE_NAME,
        Queries.StatusTable.TABLE_NAME,
        Queries.StatusMangaTable.TABLE_NAME
    )

    @Throws(IOException::class)
    private fun dumpDB(stream: OutputStream) {
        val db = database
        val writer = JsonWriter(OutputStreamWriter(stream))
        writer.beginObject()
        for (s in SCHEMAS) {
            val cur = db!!.query(s, null, null, null, null, null, null)
            writer.name(s).beginArray()
            if (cur.moveToFirst()) {
                do {
                    writer.beginObject()
                    for (i in 0 until cur.columnCount) {
                        writer.name(cur.getColumnName(i))
                        if (cur.isNull(i)) {
                            writer.nullValue()
                        } else {
                            when (cur.getType(i)) {
                                Cursor.FIELD_TYPE_INTEGER -> writer.value(cur.getLong(i))
                                Cursor.FIELD_TYPE_FLOAT -> writer.value(cur.getDouble(i))
                                Cursor.FIELD_TYPE_STRING -> writer.value(cur.getString(i))
                                Cursor.FIELD_TYPE_BLOB, Cursor.FIELD_TYPE_NULL -> {}
                            }
                        }
                    }
                    writer.endObject()
                } while (cur.moveToNext())
            }
            writer.endArray()
            cur.close()
        }
        writer.endObject()
        writer.flush()
    }

    fun defaultExportName(context: SettingsActivity?): String {
        val actualTime = Date()
        val date =
            DateFormat.getDateFormat(context).format(actualTime).replace("\\D*".toRegex(), "")
        val time =
            DateFormat.getTimeFormat(context).format(actualTime).replace("\\D*".toRegex(), "")
        return String.format("Backup_%s_%s.zip", date, time)
    }

    @Throws(IOException::class)
    fun exportData(context: SettingsActivity, selectedFile: Uri?) {
        val outputStream = context.contentResolver.openOutputStream(selectedFile!!)
        val zip = ZipOutputStream(outputStream)
        zip.setLevel(Deflater.BEST_COMPRESSION)
        zip.putNextEntry(ZipEntry(DB_ZIP_FILE))
        dumpDB(zip)
        zip.closeEntry()
        for (shared in SHARED_FILES) {
            zip.putNextEntry(ZipEntry("$shared.json"))
            exportSharedPreferences(context, shared, zip)
            zip.closeEntry()
        }
        zip.close()
    }

    @Throws(IOException::class)
    private fun exportSharedPreferences(
        context: Context,
        sharedName: String,
        stream: OutputStream
    ) {
        val writer = JsonWriter(OutputStreamWriter(stream))
        val pref = context.getSharedPreferences(sharedName, 0)
        val map = pref.all
        writer.beginObject()
        for ((key, value) in map) {
            val `val` = value!!
            writer.name(key)
            when (`val`) {
                is String -> {
                    writer.beginObject().name(SharedType.STRING.name).value(`val`).endObject()
                }
                is Boolean -> {
                    writer.beginObject().name(SharedType.BOOLEAN.name).value(`val`).endObject()
                }
                is Int -> {
                    writer.beginObject().name(SharedType.INT.name).value(`val`).endObject()
                }
                is Float -> {
                    writer.beginObject().name(SharedType.FLOAT.name).value(`val`).endObject()
                }
                is Set<*> -> {
                    writer.beginObject().name(SharedType.STRING_SET.name)
                    writer.beginArray()
                    for (s in `val` as Set<String>) {
                        writer.value(s)
                    }
                    writer.endArray()
                    writer.endObject()
                }
                is Long -> {
                    writer.beginObject().name(SharedType.LONG.name).value(`val`).endObject()
                }
                else -> {
                    error("Missing export class: " + `val`.javaClass.name)
                }
            }
        }
        writer.endObject()
        writer.flush()
    }

    internal enum class SharedType {
        FLOAT, INT, LONG, STRING_SET, STRING, BOOLEAN
    }
}
