package com.dublikunt.nclient.components.activities

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.dublikunt.nclient.async.database.DatabaseHelper
import com.dublikunt.nclient.async.downloader.DownloadGallery
import com.dublikunt.nclient.settings.Database
import com.dublikunt.nclient.settings.Global
import com.dublikunt.nclient.settings.Tags
import com.dublikunt.nclient.utility.NetworkUtil
import com.google.android.material.color.DynamicColors

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Global.initLanguage(this)
        DynamicColors.applyToActivitiesIfAvailable(this)
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        Global.initStorage(this)
        Database.database = DatabaseHelper(applicationContext).writableDatabase
        Global.initFromShared(this)
        NetworkUtil.initConnectivity(this)
        Tags.initMinCount(this)
        Tags.initSortByName(this)
        DownloadGallery.loadDownloads(this)
    }
}
