package com.dublikunt.nclientv2.components.activities;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDexApplication;

import com.dublikunt.nclientv2.async.database.DatabaseHelper;
import com.dublikunt.nclientv2.async.downloader.DownloadGalleryV2;
import com.dublikunt.nclientv2.settings.Database;
import com.dublikunt.nclientv2.settings.Global;
import com.dublikunt.nclientv2.settings.TagV2;
import com.dublikunt.nclientv2.utility.network.NetworkUtil;

public class MainApplication extends MultiDexApplication {
    @Override
    public void onCreate() {
        super.onCreate();
        Global.initLanguage(this);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        Global.initStorage(this);
        Database.setDatabase(new DatabaseHelper(getApplicationContext()).getWritableDatabase());

        Global.initFromShared(this);
        NetworkUtil.initConnectivity(this);
        TagV2.initMinCount(this);
        TagV2.initSortByName(this);
        DownloadGalleryV2.loadDownloads(this);
    }
}
