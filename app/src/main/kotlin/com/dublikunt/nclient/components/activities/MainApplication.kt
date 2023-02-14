package com.dublikunt.nclient.components.activities

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.dublikunt.nclient.async.database.DatabaseHelper
import com.dublikunt.nclient.async.downloader.DownloadGalleryV2
import com.dublikunt.nclient.settings.Database
import com.dublikunt.nclient.settings.Global
import com.dublikunt.nclient.settings.TagV2
import com.dublikunt.nclient.utility.NetworkUtil
import com.google.android.material.color.DynamicColors

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Global.initLanguage(this)
        DynamicColors.applyToActivitiesIfAvailable(this)
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        Global.initStorage(this)
        Database.setDatabase(DatabaseHelper(applicationContext).writableDatabase)
        Global.initFromShared(this)
        NetworkUtil.initConnectivity(this)
        TagV2.initMinCount(this)
        TagV2.initSortByName(this)
        DownloadGalleryV2.loadDownloads(this)
    }
}
