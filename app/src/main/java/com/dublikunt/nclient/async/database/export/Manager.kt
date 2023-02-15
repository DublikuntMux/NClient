package com.dublikunt.nclient.async.database.export

import android.net.Uri
import com.dublikunt.nclient.SettingsActivity
import com.dublikunt.nclient.utility.LogUtility.error
import java.io.IOException

class Manager(
    private val file: Uri,
    private val context: SettingsActivity,
    private val export: Boolean,
    private val end: Runnable
) : Thread() {
    override fun run() {
        try {
            if (export) Exporter.exportData(context, file) else Importer.importData(context, file)
            context.runOnUiThread(end)
        } catch (e: IOException) {
            error(e)
        }
    }
}
