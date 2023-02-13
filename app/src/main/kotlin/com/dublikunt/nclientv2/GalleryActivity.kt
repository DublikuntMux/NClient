package com.dublikunt.nclientv2

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.view.View.OnLongClickListener
import android.widget.ArrayAdapter
import android.widget.CheckedTextView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import com.dublikunt.nclientv2.CopyToClipboardActivity.Companion.copyTextToClipboard
import com.dublikunt.nclientv2.adapters.GalleryAdapter
import com.dublikunt.nclientv2.api.InspectorV3
import com.dublikunt.nclientv2.api.InspectorV3.DefaultInspectorResponse
import com.dublikunt.nclientv2.api.components.Gallery
import com.dublikunt.nclientv2.api.components.GenericGallery
import com.dublikunt.nclientv2.async.database.Queries
import com.dublikunt.nclientv2.components.activities.BaseActivity
import com.dublikunt.nclientv2.components.status.StatusManager
import com.dublikunt.nclientv2.components.views.RangeSelector
import com.dublikunt.nclientv2.components.widgets.CustomGridLayoutManager
import com.dublikunt.nclientv2.settings.*
import com.dublikunt.nclientv2.utility.LogUtility
import com.dublikunt.nclientv2.utility.Utility
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.skydoves.colorpickerview.ColorEnvelope
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.util.*

class GalleryActivity : BaseActivity() {
    private var gallery: GenericGallery = Gallery.emptyGallery()
    private var isLocal = false
    private lateinit var adapter: GalleryAdapter
    private var zoom = 0
    private var isLocalFavorite = false
    private lateinit var toolbar: MaterialToolbar
    private lateinit var onlineFavoriteItem: MenuItem
    private var statusString: String? = null
    private var newStatusColor = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)
        if (Global.isLockScreen()) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowTitleEnabled(true)
        recycler = findViewById(R.id.recycler)
        refresher = findViewById(R.id.refresher)
        masterLayout = findViewById(R.id.master_layout)
        val gal = intent.getParcelableExtra<GenericGallery>("$packageName.GALLERY")
        if (gal == null && !tryLoadFromURL()) {
            finish()
            return
        }
        if (gal != null) gallery = gal
        if (gallery.type != GenericGallery.Type.LOCAL) {
            Queries.HistoryTable.addGallery((gallery as Gallery).toSimpleGallery())
        }
        LogUtility.download("" + gallery)
        if (Global.useRtl()) recycler.rotationY = 180f
        isLocal = intent.getBooleanExtra("$packageName.ISLOCAL", false)
        zoom = intent.getIntExtra("$packageName.ZOOM", 0)
        refresher.isEnabled = false
        recycler.layoutManager =
            CustomGridLayoutManager(this, Global.getColumnCount())
        loadGallery(gallery, zoom) //if already has gallery
    }

    private fun tryLoadFromURL(): Boolean {
        val data = intent.data
        if (data != null && data.pathSegments.size >= 2) { //if using an URL
            val params = data.pathSegments
            LogUtility.download(params.size.toString() + ": " + params)
            val id: Int = try { //if not an id return
                params[1].toInt()
            } catch (ignore: NumberFormatException) {
                return false
            }
            if (params.size > 2) { //check if it has a specific page
                zoom = try {
                    params[2].toInt()
                } catch (e: NumberFormatException) {
                    e.localizedMessage?.let { LogUtility.error(it) }
                    0
                }
            }
            InspectorV3.galleryInspector(this, id, object : DefaultInspectorResponse() {
                override fun onSuccess(galleries: List<GenericGallery>) {
                    if (galleries.isNotEmpty()) {
                        val intent = Intent(this@GalleryActivity, GalleryActivity::class.java)
                        intent.putExtra("$packageName.GALLERY", galleries[0])
                        intent.putExtra("$packageName.ZOOM", zoom)
                        startActivity(intent)
                    }
                    finish()
                }
            }).start()
            return true
        }
        return false
    }

    private fun lookup() {
        val manager = recycler.layoutManager as CustomGridLayoutManager?
        val adapter = recycler.adapter as GalleryAdapter?
        manager!!.spanSizeLookup = object : SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (adapter!!.positionToType(position) == GalleryAdapter.Type.PAGE) 1 else manager.spanCount
            }
        }
    }

    private fun loadGallery(gall: GenericGallery, zoom: Int) {
        gallery = gall
        if (supportActionBar != null) {
            applyTitle()
        }
        adapter = GalleryAdapter(this, gallery, Global.getColumnCount())
        recycler.adapter = adapter
        lookup()
        if (zoom > 0 && Global.getDownloadPolicy() != Global.DataUsageType.NONE) {
            val intent = Intent(this, ZoomActivity::class.java)
            intent.putExtra("$packageName.GALLERY", gallery)
            intent.putExtra("$packageName.DIRECTORY", adapter.directory)
            intent.putExtra("$packageName.PAGE", zoom)
            startActivity(intent)
        }
        checkBookmark()
    }

    private fun checkBookmark() {
        val page = Queries.ResumeTable.pageFromId(gallery.id)
        if (page < 0) return
        val snack = Snackbar.make(
            toolbar,
            getString(R.string.resume_from_page, page),
            Snackbar.LENGTH_LONG
        )
        //Should be already compensated
        snack.setAction(R.string.resume) { v: View? ->
            Thread(Runnable {
                runOnUiThread { recycler.scrollToPosition(page) }
                if (Global.getColumnCount() != 1) return@Runnable
                Utility.threadSleep(500)
                runOnUiThread { recycler.scrollToPosition(page) }
            }).start()
        }
        snack.show()
    }

    @SuppressLint("RestrictedApi")
    private fun applyTitle() {
        val collapsing = findViewById<CollapsingToolbarLayout>(R.id.collapsing)
        val actionBar = supportActionBar
        val title = gallery.title
        if (collapsing == null || actionBar == null) return
        val listener = OnLongClickListener { v: View? ->
            copyTextToClipboard(this@GalleryActivity, title)
            runOnUiThread {
                Toast.makeText(
                    this@GalleryActivity,
                    R.string.title_copied_to_clipboard,
                    Toast.LENGTH_SHORT
                ).show()
            }
            true
        }
        collapsing.setOnLongClickListener(listener)
        findViewById<View>(R.id.toolbar).setOnLongClickListener(listener)
        if (title.length > 100) {
            collapsing.setExpandedTitleTextAppearance(android.R.style.TextAppearance_DeviceDefault_Medium)
            collapsing.maxLines = 5
        } else {
            collapsing.setExpandedTitleTextAppearance(android.R.style.TextAppearance_DeviceDefault_Large)
            collapsing.maxLines = 4
        }
        actionBar.title = title
    }


    override val portraitColumnCount: Int
        get() {
            return 0
        }

    override val landscapeColumnCount: Int
        get() {
            return 0
        }

    private fun initFavoriteIcon(menu: Menu) {
        val onlineFavorite = !isLocal && (gallery as Gallery).isOnlineFavorite
        val unknown = intent.getBooleanExtra("$packageName.UNKNOWN", false)
        val item = menu.findItem(R.id.add_online_gallery)
        item.setIcon(if (onlineFavorite) R.drawable.ic_star else R.drawable.ic_star_border)
        if (unknown) item.setTitle(R.string.toggle_online_favorite) else if (onlineFavorite) item.setTitle(
            R.string.remove_from_online_favorites
        ) else item.setTitle(R.string.add_to_online_favorite)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.gallery, menu)
        isLocalFavorite = Favorites.isFavorite(gallery)
        menu.findItem(R.id.favorite_manager)
            .setIcon(if (isLocalFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border)
        menuItemsVisible(menu)
        initFavoriteIcon(menu)
        Utility.tintMenu(menu)
        updateColumnCount(false)
        return true
    }

    private fun menuItemsVisible(menu: Menu) {
        val isLogged = Login.isLogged()
        val isValidOnline = gallery.isValid && !isLocal
        onlineFavoriteItem = menu.findItem(R.id.add_online_gallery)
        onlineFavoriteItem.isVisible = isValidOnline && isLogged
        menu.findItem(R.id.favorite_manager).isVisible = isValidOnline
        menu.findItem(R.id.download_gallery).isVisible = isValidOnline
        menu.findItem(R.id.related).isVisible = isValidOnline
        menu.findItem(R.id.comments).isVisible = isValidOnline
        menu.findItem(R.id.download_torrent).isVisible = isLogged
        menu.findItem(R.id.share).isVisible = gallery.isValid
        menu.findItem(R.id.load_internet).isVisible = isLocal && gallery.isValid
    }

    override fun onResume() {
        super.onResume()
        updateColumnCount(false)
        if (isLocal) supportInvalidateOptionsMenu()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.download_gallery) {
            if (Global.hasStoragePermission(this)) RangeSelector(
                this,
                gallery as Gallery
            ).show() else requestStorage()
        } else if (id == R.id.add_online_gallery) addToFavorite(item) else if (id == R.id.change_view) updateColumnCount(
            true
        ) else if (id == R.id.download_torrent) downloadTorrent() else if (id == R.id.load_internet) toInternet() else if (id == R.id.manage_status) updateStatus() else if (id == R.id.share) Global.shareGallery(
            this,
            gallery
        ) else if (id == R.id.comments) {
            val i = Intent(this, CommentActivity::class.java)
            i.putExtra("$packageName.GALLERYID", gallery.id)
            startActivity(i)
        } else if (id == R.id.related) {
            recycler.smoothScrollToPosition(recycler.adapter!!.itemCount)
        } else if (id == R.id.favorite_manager) {
            if (isLocalFavorite) {
                if (Favorites.removeFavorite(gallery)) isLocalFavorite = !isLocalFavorite
            } else if (Favorites.addFavorite(gallery as Gallery)) {
                isLocalFavorite = !isLocalFavorite
            }
            item.setIcon(if (isLocalFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border)
            Global.setTint(item.icon)
        } else if (id == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun downloadTorrent() {
        if (!Global.hasStoragePermission(this)) {
            return
        }
        val url = String.format(Locale.US, Utility.getBaseUrl() + "g/%d/download", gallery.id)
        val referer = String.format(Locale.US, Utility.getBaseUrl() + "g/%d/", gallery.id)
        AuthRequest(referer, url, object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(
                        this@GalleryActivity,
                        R.string.failed,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                val file = File(Global.TORRENTFOLDER, gallery.id.toString() + ".torrent")
                Utility.writeStreamToFile(response.body.byteStream(), file)
                val intent = Intent(Intent.ACTION_VIEW)
                val torrentUri: Uri = FileProvider.getUriForFile(
                    this@GalleryActivity,
                    this@GalleryActivity.packageName + ".provider",
                    file
                )
                intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                intent.setDataAndType(torrentUri, "application/x-bittorrent")
                try {
                    this@GalleryActivity.startActivity(intent)
                } catch (ignore: RuntimeException) {
                    runOnUiThread {
                        Toast.makeText(
                            this@GalleryActivity,
                            R.string.failed,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                file.deleteOnExit()
            }
        }).setMethod("GET", null).start()
    }

    private fun updateStatus() {
        val statuses = StatusManager.getNames()
        val builder = MaterialAlertDialogBuilder(this)
        statusString = Queries.StatusMangaTable.getStatus(gallery.id).name
        val adapter: ArrayAdapter<String?> = object :
            ArrayAdapter<String?>(this, android.R.layout.select_dialog_singlechoice, statuses) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val textView = super.getView(position, convertView, parent) as CheckedTextView
                textView.setTextColor(StatusManager.getByName(statuses[position]).opaqueColor())
                return textView
            }
        }
        builder.setSingleChoiceItems(
            adapter,
            statuses.indexOf(statusString)
        ) { _: DialogInterface?, which: Int -> statusString = statuses[which] }
        builder
            .setNeutralButton(R.string.add) { _: DialogInterface?, _: Int -> createNewStatusDialog() }
            .setNegativeButton(R.string.remove_status) { _: DialogInterface?, _: Int ->
                Queries.StatusMangaTable.remove(
                    gallery.id
                )
            }
            .setPositiveButton(R.string.ok) { _: DialogInterface?, _: Int ->
                Queries.StatusMangaTable.insert(
                    gallery,
                    statusString
                )
            }
            .setTitle(R.string.change_status_title)
            .show()
    }

    private fun createNewStatusDialog() {
        val builder = MaterialAlertDialogBuilder(this)
        val layout = View.inflate(this, R.layout.dialog_add_status, null) as LinearLayout
        val name = layout.findViewById<TextInputEditText>(R.id.name)
        val btnColor = layout.findViewById<MaterialButton>(R.id.color)
        do {
            newStatusColor = Utility.RANDOM.nextInt() or -0x1000000
        } while (newStatusColor == Color.BLACK || newStatusColor == Color.WHITE)
        btnColor.setBackgroundColor(newStatusColor)
        btnColor.setOnClickListener { v: View? ->
            ColorPickerDialog.Builder(this)
                .setTitle(R.string.сolor_selection)
                .setPositiveButton(getString(R.string.confirm),
                    ColorEnvelopeListener { envelope: ColorEnvelope, _: Boolean ->
                        if (envelope.color == Color.WHITE || envelope.color == Color.BLACK) {
                            Toast.makeText(
                                this@GalleryActivity,
                                R.string.invalid_color_selected,
                                Toast.LENGTH_SHORT
                            ).show()
                            return@ColorEnvelopeListener
                        }
                        newStatusColor = envelope.color
                        btnColor.setBackgroundColor(envelope.color)
                    })
                .setNegativeButton(
                    getString(R.string.cancel)
                ) { dialogInterface: DialogInterface, _: Int -> dialogInterface.dismiss() }
                .attachAlphaSlideBar(false)
                .attachBrightnessSlideBar(true)
                .setBottomSpace(12)
                .show()
        }
        builder.setView(layout)
        builder.setTitle(R.string.create_new_status)
        builder.setPositiveButton(R.string.ok) { _: DialogInterface?, _: Int ->
            val newName = name.text.toString()
            if (newName.length < 2) {
                Toast.makeText(this, R.string.name_too_short, Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            if (StatusManager.getByName(newName) != null) {
                Toast.makeText(this, R.string.duplicated_name, Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            val status = StatusManager.add(name.text.toString(), newStatusColor)
            Queries.StatusMangaTable.insert(gallery, status)
        }
        builder.setNegativeButton(R.string.cancel) { _: DialogInterface?, _: Int -> updateStatus() }
        builder.setOnCancelListener { updateStatus() }
        builder.show()
    }

    private fun updateIcon(nowIsFavorite: Boolean) {
        runOnUiThread {
            onlineFavoriteItem.setIcon(if (!nowIsFavorite) R.drawable.ic_star_border else R.drawable.ic_star)
            onlineFavoriteItem.setTitle(if (!nowIsFavorite) R.string.add_to_online_favorite else R.string.remove_from_online_favorites)
        }
    }

    private fun addToFavorite(item: MenuItem) {
        val wasFavorite =
            onlineFavoriteItem.title == getString(R.string.remove_from_online_favorites)
        val url = String.format(
            Locale.US,
            Utility.getBaseUrl() + "api/gallery/%d/%sfavorite",
            gallery.id,
            if (wasFavorite) "un" else ""
        )
        val galleryUrl = String.format(Locale.US, Utility.getBaseUrl() + "g/%d/", gallery.id)
        LogUtility.download("Calling: $url")
        AuthRequest(galleryUrl, url, object : Callback {
            override fun onFailure(call: Call, e: IOException) {}

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                val responseString = response.body.string()
                val nowIsFavorite = responseString.contains("true")
                updateIcon(nowIsFavorite)
            }
        }).setMethod("POST", AuthRequest.EMPTY_BODY).start()
    }

    private fun updateColumnCount(increase: Boolean) {
        var x = Global.getColumnCount()
        val manager = recycler.layoutManager as CustomGridLayoutManager? ?: return
        val item =
            (findViewById<View>(R.id.toolbar) as MaterialToolbar).menu.findItem(R.id.change_view)
        if (increase || manager.spanCount != x) {
            if (increase) x = x % 4 + 1
            val pos = manager.findFirstVisibleItemPosition()
            Global.updateColumnCount(this, x)
            recycler.layoutManager = CustomGridLayoutManager(this, x)
            LogUtility.download("Span count: " + manager.spanCount)
            adapter.setColCount(Global.getColumnCount())
            recycler.adapter = adapter
            lookup()
            recycler.scrollToPosition(pos)
            adapter.setMaxImageSize(null)
        }
        if (item != null) {
            when (x) {
                1 -> item.setIcon(R.drawable.ic_view_1)
                2 -> item.setIcon(R.drawable.ic_view_2)
                3 -> item.setIcon(R.drawable.ic_view_3)
                4 -> item.setIcon(R.drawable.ic_view_4)
            }
            Global.setTint(item.icon)
        }
    }

    private fun toInternet() {
        refresher.isEnabled = true
        InspectorV3.galleryInspector(this, gallery.id, object : DefaultInspectorResponse() {
            override fun onSuccess(galleries: List<GenericGallery>) {
                if (galleries.isEmpty()) return
                val intent = Intent(this@GalleryActivity, GalleryActivity::class.java)
                LogUtility.download(galleries[0].toString())
                intent.putExtra("$packageName.GALLERY", galleries[0])
                runOnUiThread { startActivity(intent) }
            }
        }).start()
    }

    private fun requestStorage() {
        requestPermissions(
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ), 1
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Global.initStorage(this)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) RangeSelector(
            this,
            gallery as Gallery
        ).show()
    }
}
