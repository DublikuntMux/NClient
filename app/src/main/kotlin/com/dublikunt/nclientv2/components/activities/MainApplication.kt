package com.dublikunt.nclientv2.components.activities

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.dublikunt.nclientv2.async.database.DatabaseHelper
import com.dublikunt.nclientv2.async.downloader.DownloadGalleryV2
import com.dublikunt.nclientv2.settings.Database
import com.dublikunt.nclientv2.settings.Global
import com.dublikunt.nclientv2.settings.TagV2
import com.dublikunt.nclientv2.utility.NetworkUtil

class MainApplication : Application() {
    private lateinit var appObserver: ForegroundBackgroundListener

    override fun onCreate() {
        super.onCreate()
        Global.initLanguage(this)
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        Global.initStorage(this)
        Database.setDatabase(DatabaseHelper(applicationContext).writableDatabase)
        Global.initFromShared(this)
        NetworkUtil.initConnectivity(this)
        TagV2.initMinCount(this)
        TagV2.initSortByName(this)
        DownloadGalleryV2.loadDownloads(this)
        /*
        ProcessLifecycleOwner.get()
            .lifecycle
            .addObserver(
                ForegroundBackgroundListener()
                    .also { appObserver = it })
         */
    }
}

internal class ForegroundBackgroundListener : LifecycleEventObserver {
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_START -> TODO("Delete blur")
            Lifecycle.Event.ON_STOP -> TODO("Add blur")
            else -> {}
        }
    }
}
