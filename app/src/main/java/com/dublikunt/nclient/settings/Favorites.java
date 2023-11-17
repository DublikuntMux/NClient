package com.dublikunt.nclient.settings;

import androidx.annotation.NonNull;

import com.dublikunt.nclient.api.components.Gallery;
import com.dublikunt.nclient.api.components.GenericGallery;
import com.dublikunt.nclient.async.database.Queries;

public class Favorites {
    public static boolean addFavorite(Gallery gallery) {
        Queries.FavoriteTable.addFavorite(gallery);
        return true;
    }

    public static boolean removeFavorite(@NonNull GenericGallery gallery) {
        Queries.FavoriteTable.removeFavorite(gallery.getId());
        return true;
    }

    public static boolean isFavorite(GenericGallery gallery) {
        if (gallery == null || !gallery.isValid()) return false;
        return Queries.FavoriteTable.isFavorite(gallery.getId());
    }
}
