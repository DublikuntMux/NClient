package com.dublikunt.nclient.settings

import com.dublikunt.nclient.api.gallerys.Gallery
import com.dublikunt.nclient.api.gallerys.GenericGallery
import com.dublikunt.nclient.async.database.Queries

object Favorites {
    fun addFavorite(gallery: Gallery): Boolean {
        Queries.FavoriteTable.addFavorite(gallery)
        return true
    }

    fun removeFavorite(gallery: GenericGallery): Boolean {
        Queries.FavoriteTable.removeFavorite(gallery.id)
        return true
    }

    fun isFavorite(gallery: GenericGallery?): Boolean {
        return if (gallery == null || !gallery.valid) false else Queries.FavoriteTable.isFavorite(
            gallery.id
        )
    }
}
