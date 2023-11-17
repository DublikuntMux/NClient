package com.dublikunt.nclient.components.activities;

import android.app.Application;
import android.os.Build;

import androidx.appcompat.app.AppCompatDelegate;

import com.dublikunt.nclient.async.database.DatabaseHelper;
import com.dublikunt.nclient.async.downloader.DownloadGallery;
import com.dublikunt.nclient.settings.Database;
import com.dublikunt.nclient.settings.Global;
import com.dublikunt.nclient.settings.Tag;
import com.dublikunt.nclient.utility.network.NetworkUtil;
import com.google.android.material.color.DynamicColors;


public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Global.initLanguage(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            DynamicColors.applyToActivitiesIfAvailable(this);
        }
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        Global.initStorage(this);
        Database.setDatabase(new DatabaseHelper(getApplicationContext()).getWritableDatabase());
        Global.initFromShared(this);
        NetworkUtil.initConnectivity(this);
        Tag.initMinCount(this);
        Tag.initSortByName(this);
        DownloadGallery.loadDownloads(this);
    }
}
