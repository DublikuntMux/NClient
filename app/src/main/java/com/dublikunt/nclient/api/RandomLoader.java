package com.dublikunt.nclient.api;

import com.dublikunt.nclient.RandomActivity;
import com.dublikunt.nclient.api.components.Gallery;
import com.dublikunt.nclient.api.components.GenericGallery;
import com.dublikunt.nclient.utility.ImageDownloadUtility;

import java.util.ArrayList;
import java.util.List;

public class RandomLoader {
    private static final int MAXLOADED = 5;
    private final List<Gallery> galleries;
    private final RandomActivity activity;
    private boolean galleryHasBeenRequested;

    public RandomLoader(RandomActivity activity) {
        this.activity = activity;
        galleries = new ArrayList<>(MAXLOADED);
        galleryHasBeenRequested = RandomActivity.loadedGallery == null;
        loadRandomGallery();
    }

    /**
    * Loads a random gallery from the web. If there are too many galleries this is a no - op
    */
    private void loadRandomGallery() {
        // Returns true if the galleries are not loaded.
        if (galleries.size() >= MAXLOADED) return;
        Inspector.randomInspector(activity, response, false).start();
    }

    /**
    * Requests a gallery from the gallery manager. This is called by the Activity#onCreate ( Bundle ) method
    */
    public void requestGallery() {
        galleryHasBeenRequested = true;
        // Removes all galleries from the list.
        for (int i = 0; i < galleries.size(); i++) {
            // Remove the galleries at the specified index.
            if (galleries.get(i) == null)
                galleries.remove(i--);
        }
        // Load the gallery from the list of galleries.
        if (galleries.size() > 0) {
            Gallery gallery = galleries.remove(0);
            activity.runOnUiThread(() -> activity.loadGallery(gallery));
            galleryHasBeenRequested = false;
        }
        loadRandomGallery();
    }

    private final Inspector.InspectorResponse response = new Inspector.DefaultInspectorResponse() {
        /**
        * Called when the request failed. This is the place to do anything that needs to be done in response to an HTTP request.
        *
        * @param e - The exception that caused the request to fail. May be null
        */
        @Override
        public void onFailure(Exception e) {
            loadRandomGallery();
        }

        /**
        * Called when the list of galleries is loaded. This is a callback method that will be called by the gallery manager when it has finished loading a gallery.
        *
        * @param galleryList - The list of galleries that was loaded
        */
        @Override
        public void onSuccess(List<GenericGallery> galleryList) {
            // Load random gallery if not empty.
            if (galleryList.size() == 0 || !galleryList.get(0).isValid()) {
                loadRandomGallery();
                return;
            }
            Gallery gallery = (Gallery) galleryList.get(0);
            galleries.add(gallery);
            ImageDownloadUtility.preloadImage(activity, gallery.getCover());
            // Requests the gallery if the gallery has been requested.
            if (galleryHasBeenRequested)
                requestGallery();//requestGallery will call loadRandomGallery
            // Load random gallery if the number of galleries is less than MAXLOADED
            else if (galleries.size() < MAXLOADED) loadRandomGallery();
        }
    };


}
