package com.dublikunt.nclient.api.local;

import androidx.annotation.NonNull;

import com.dublikunt.nclient.LocalActivity;
import com.dublikunt.nclient.adapters.LocalAdapter;
import com.dublikunt.nclient.components.ThreadAsyncTask;
import com.dublikunt.nclient.utility.LogUtility;

import java.io.File;
import java.util.ArrayList;

public class FakeInspector extends ThreadAsyncTask<LocalActivity, LocalActivity, LocalActivity> {
    private final ArrayList<LocalGallery> galleries;
    private final ArrayList<String> invalidPaths;
    private final File folder;

    public FakeInspector(LocalActivity activity, File folder) {
        super(activity);
        this.folder = new File(folder, "Download");
        galleries = new ArrayList<>();
        invalidPaths = new ArrayList<>();
    }

    @Override
    protected LocalActivity doInBackground(LocalActivity... voids) {
        if (!this.folder.exists()) return voids[0];
        publishProgress(voids[0]);
        File parent = this.folder;
        parent.mkdirs();
        File[] files = parent.listFiles();
        if (files == null) return voids[0];
        for (File f : files) if (f.isDirectory()) createGallery(f);
        for (String x : invalidPaths) LogUtility.d("Invalid path: " + x);
        return voids[0];
    }

    @Override
    protected void onProgressUpdate(@NonNull LocalActivity... values) {
        values[0].getRefresher().setRefreshing(true);
    }

    @Override
    protected void onPostExecute(@NonNull LocalActivity aVoid) {
        aVoid.getRefresher().setRefreshing(false);
        aVoid.setAdapter(new LocalAdapter(aVoid, galleries));
    }

    private void createGallery(final File file) {
        LocalGallery lg = new LocalGallery(file, true);
        if (lg.isValid()) {
            galleries.add(lg);
        } else {
            LogUtility.e(lg);
            invalidPaths.add(file.getAbsolutePath());
        }
    }
}
