package com.dublikunt.nclient.api

import com.dublikunt.nclient.LocalActivity
import com.dublikunt.nclient.adapters.LocalAdapter
import com.dublikunt.nclient.api.gallerys.LocalGallery
import com.dublikunt.nclient.components.ThreadAsyncTask
import com.dublikunt.nclient.utility.LogUtility.download
import com.dublikunt.nclient.utility.LogUtility.error
import java.io.File

class FakeInspector(activity: LocalActivity, folder: File) :
    ThreadAsyncTask<LocalActivity, LocalActivity, LocalActivity>(
        activity
    ) {
    private val galleries: ArrayList<LocalGallery>
    private val invalidPaths: ArrayList<String>
    private val folder: File

    init {
        this.folder = File(folder, "Download")
        galleries = ArrayList()
        invalidPaths = ArrayList()
    }

    override fun doInBackground(vararg params: LocalActivity): LocalActivity {
        if (!folder.exists()) return params[0]
        publishProgress(params[0])
        val parent = folder
        parent.mkdirs()
        val files = parent.listFiles() ?: return params[0]
        for (f in files) if (f.isDirectory) createGallery(f)
        for (x in invalidPaths) download("Invalid path: $x")
        return params[0]
    }

    override fun onProgressUpdate(vararg values: LocalActivity) {
        values[0].refresher.isRefreshing = true
    }

    override fun onPostExecute(result: LocalActivity) {
        result.refresher.isRefreshing = false
        result.setAdapter(LocalAdapter(result, galleries))
    }

    private fun createGallery(file: File) {
        val lg = LocalGallery(file, true)
        if (lg.valid) {
            galleries.add(lg)
        } else {
            error(lg)
            invalidPaths.add(file.absolutePath)
        }
    }
}
