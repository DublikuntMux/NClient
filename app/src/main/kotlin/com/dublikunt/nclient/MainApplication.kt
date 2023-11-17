package com.dublikunt.nclient

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.dublikunt.nclient.async.database.DatabaseHelper
import com.dublikunt.nclient.async.downloader.DownloadGallery
import com.dublikunt.nclient.settings.Database
import com.dublikunt.nclient.settings.Global
import com.dublikunt.nclient.settings.Tag
import com.dublikunt.nclient.utility.NetworkUtil

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Global.initLanguage(this)
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        Global.initStorage(this)
        Database.setDatabase(DatabaseHelper(applicationContext).writableDatabase)
        Global.initFromShared(this)
        NetworkUtil.initConnectivity(this)
        Tag.initMinCount(this)
        Tag.initSortByName(this)
        DownloadGallery.loadDownloads(this)
    }
}
