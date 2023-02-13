package com.dublikunt.nclientv2

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.widget.ImageViewCompat
import com.dublikunt.nclientv2.api.RandomLoader
import com.dublikunt.nclientv2.api.components.Gallery
import com.dublikunt.nclientv2.components.activities.GeneralActivity
import com.dublikunt.nclientv2.settings.Favorites
import com.dublikunt.nclientv2.settings.Global
import com.dublikunt.nclientv2.utility.ImageDownloadUtility
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton

class RandomActivity : GeneralActivity() {
    private lateinit var language: TextView
    private lateinit var thumbnail: ImageButton
    private lateinit var favorite: ImageButton
    private lateinit var title: TextView
    private lateinit var page: TextView
    private lateinit var censor: View
    private var loader: RandomLoader? = null
    private var isFavorite = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_random)
        loader = RandomLoader(this)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        //init components id
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        val shuffle = findViewById<FloatingActionButton>(R.id.shuffle)
        val share = findViewById<ImageButton>(R.id.share)
        censor = findViewById(R.id.censor)
        language = findViewById(R.id.language)
        thumbnail = findViewById(R.id.thumbnail)
        favorite = findViewById(R.id.favorite)
        title = findViewById(R.id.title)
        page = findViewById(R.id.pages)

        //init toolbar
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowTitleEnabled(true)
        supportActionBar!!.setTitle(R.string.random_manga)
        if (loadedGallery != null) loadGallery(loadedGallery)
        shuffle.setOnClickListener { loader!!.requestGallery() }
        thumbnail.setOnClickListener {
            if (loadedGallery != null) {
                val intent = Intent(this@RandomActivity, GalleryActivity::class.java)
                intent.putExtra(this@RandomActivity.packageName + ".GALLERY", loadedGallery)
                this@RandomActivity.startActivity(intent)
            }
        }
        share.setOnClickListener {
            if (loadedGallery != null) Global.shareGallery(
                this@RandomActivity,
                loadedGallery
            )
        }
        censor.setOnClickListener { censor.visibility = View.GONE }
        favorite.setOnClickListener {
            if (loadedGallery != null) {
                if (isFavorite) {
                    if (Favorites.removeFavorite(loadedGallery)) isFavorite = false
                } else if (Favorites.addFavorite(loadedGallery)) isFavorite = true
            }
            favoriteUpdateButton()
        }
        val colorStateList =
            ColorStateList.valueOf(if (Global.getTheme() == Global.ThemeScheme.LIGHT) Color.WHITE else Color.BLACK)
        ImageViewCompat.setImageTintList(shuffle, colorStateList)
        ImageViewCompat.setImageTintList(share, colorStateList)
        ImageViewCompat.setImageTintList(favorite, colorStateList)
        Global.setTint(shuffle.contentBackground)
        Global.setTint(share.drawable)
        Global.setTint(favorite.drawable)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    fun loadGallery(gallery: Gallery?) {
        loadedGallery = gallery
        if (Global.isDestroyed(this)) return
        ImageDownloadUtility.loadImage(this, gallery!!.cover, thumbnail)
        language.text = Global.getLanguageFlag(gallery.language)
        isFavorite = Favorites.isFavorite(loadedGallery)
        favoriteUpdateButton()
        title.text = gallery.title
        page.text = getString(R.string.page_count_format, gallery.pageCount)
        censor.visibility =
            if (gallery.hasIgnoredTags()) View.VISIBLE else View.GONE
    }

    private fun favoriteUpdateButton() {
        runOnUiThread {
            ImageDownloadUtility.loadImage(
                if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border,
                favorite
            )
            Global.setTint(favorite.drawable)
        }
    }


    private val onBackPressedCallback: OnBackPressedCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                loadedGallery = null
            }
        }

    companion object {
        @JvmField
        var loadedGallery: Gallery? = null
    }
}
