package com.dublikunt.nclient.async.database.export

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.util.JsonReader
import android.util.JsonToken
import com.dublikunt.nclient.async.database.export.Exporter.SharedType
import com.dublikunt.nclient.settings.Database.database
import com.dublikunt.nclient.utility.LogUtility.download
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

internal object Importer {
    @Throws(IOException::class)
    private fun importSharedPreferences(context: Context, sharedName: String, stream: InputStream) {
        val reader = JsonReader(InputStreamReader(stream))
        val editor = context.getSharedPreferences(sharedName, 0).edit()
        reader.beginObject()
        while (reader.hasNext()) {
            val name = reader.nextName()
            reader.beginObject()
            when (SharedType.valueOf(reader.nextName())) {
                SharedType.STRING -> editor.putString(name, reader.nextString())
                SharedType.INT -> editor.putInt(name, reader.nextInt())
                SharedType.FLOAT -> editor.putFloat(name, reader.nextDouble().toFloat())
                SharedType.LONG -> editor.putLong(name, reader.nextLong())
                SharedType.BOOLEAN -> editor.putBoolean(name, reader.nextBoolean())
                SharedType.STRING_SET -> {
                    val strings: MutableSet<String> = HashSet()
                    reader.beginArray()
                    while (reader.hasNext()) strings.add(reader.nextString())
                    reader.endArray()
                    editor.putStringSet(name, strings)
                }
            }
            reader.endObject()
        }
        editor.apply()
    }

    @Throws(IOException::class)
    private fun importDB(stream: InputStream) {
        val db = database
        db!!.beginTransaction()
        val reader = JsonReader(InputStreamReader(stream))
        reader.beginObject()
        while (reader.hasNext()) {
            val tableName = reader.nextName()
            db.delete(tableName, null, null)
            reader.beginArray()
            while (reader.hasNext()) {
                reader.beginObject()
                val values = ContentValues()
                while (reader.hasNext()) {
                    val fieldName = reader.nextName()
                    when (reader.peek()) {
                        JsonToken.NULL -> {
                            values.putNull(fieldName)
                            reader.nextNull()
                        }

                        JsonToken.NUMBER ->
                            values.put(fieldName, reader.nextLong())

                        JsonToken.STRING -> values.put(fieldName, reader.nextString())
                        else -> {
                            values.putNull(fieldName)
                            reader.nextNull()
                        }
                    }
                }
                db.insertWithOnConflict(tableName, null, values, SQLiteDatabase.CONFLICT_REPLACE)
                reader.endObject()
            }
            reader.endArray()
        }
        reader.endObject()
        db.setTransactionSuccessful()
        db.endTransaction()
    }

    @Throws(IOException::class)
    fun importData(context: Context, selectedFile: Uri?) {
        val stream = context.contentResolver.openInputStream(selectedFile!!)
        val inputStream = ZipInputStream(stream)
        var entry: ZipEntry
        while (inputStream.nextEntry.also { entry = it } != null) {
            val name = entry.name
            download("Importing: $name")
            if (Exporter.DB_ZIP_FILE == name) {
                importDB(inputStream)
            } else {
                val shared = name.substring(0, name.length - 5)
                importSharedPreferences(context, shared, inputStream)
            }
            inputStream.closeEntry()
        }
        inputStream.close()
    }
}
