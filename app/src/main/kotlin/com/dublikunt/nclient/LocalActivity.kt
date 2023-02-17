package com.dublikunt.nclient

import android.content.DialogInterface
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import androidx.appcompat.widget.SearchView
import com.dublikunt.nclient.adapters.LocalAdapter
import com.dublikunt.nclient.api.FakeInspector
import com.dublikunt.nclient.api.LocalGallery
import com.dublikunt.nclient.api.LocalSortType
import com.dublikunt.nclient.async.converters.CreatePDF
import com.dublikunt.nclient.async.downloader.GalleryDownloader
import com.dublikunt.nclient.classes.MultichoiceAdapter
import com.dublikunt.nclient.classes.MultichoiceAdapter.DefaultMultichoiceListener
import com.dublikunt.nclient.classes.MultichoiceAdapter.MultichoiceListener
import com.dublikunt.nclient.components.activities.BaseActivity
import com.dublikunt.nclient.settings.Global
import com.dublikunt.nclient.utility.Utility
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import java.io.File

class LocalActivity : BaseActivity() {
    private var optionMenu: Menu? = null
    private var adapter: LocalAdapter? = null
    private val listener: MultichoiceListener = object : DefaultMultichoiceListener() {
        override fun choiceChanged() {
            setMenuVisibility(optionMenu)
        }
    }
    private lateinit var toolbar: MaterialToolbar
    var colCount = 0
        private set
    private var idGalleryPosition = -1
    private var folder = Global.MAINFOLDER
    private var searchView: SearchView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.app_bar_main)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowTitleEnabled(true)
        supportActionBar!!.setTitle(R.string.downloaded_manga)
        findViewById<View>(R.id.page_switcher).visibility = View.GONE
        recycler = findViewById(R.id.recycler)
        refresher = findViewById(R.id.refresher)
        refresher.setOnRefreshListener { FakeInspector(this, folder).execute(this) }
        changeLayout(resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
        FakeInspector(this, folder).execute(this)
    }

    fun setAdapter(adapter: LocalAdapter?) {
        this.adapter = adapter
        this.adapter!!.addListener(listener)
        recycler.adapter = adapter
    }

    fun setIdGalleryPosition(idGalleryPosition: Int) {
        this.idGalleryPosition = idGalleryPosition
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.download, menu)
        menuInflater.inflate(R.menu.local_multichoice, menu)
        optionMenu = menu
        setMenuVisibility(menu)
        searchView = menu.findItem(R.id.search).actionView as SearchView
        searchView!!.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if (recycler.adapter != null) (recycler.adapter as LocalAdapter?)!!.filter.filter(
                    newText
                )
                return true
            }
        })
        Utility.tintMenu(menu)
        return true
    }

    private fun setMenuVisibility(menu: Menu?) {
        if (menu == null) return
        val mode = if (adapter == null) MultichoiceAdapter.Mode.NORMAL else adapter!!.mode
        var hasGallery = false
        var hasDownloads = false
        if (mode == MultichoiceAdapter.Mode.SELECTING) {
            hasGallery = adapter!!.hasSelectedClass(LocalGallery::class.java)
            hasDownloads = adapter!!.hasSelectedClass(GalleryDownloader::class.java)
        }
        menu.findItem(R.id.search).isVisible = mode == MultichoiceAdapter.Mode.NORMAL
        menu.findItem(R.id.sort_by_name).isVisible = mode == MultichoiceAdapter.Mode.NORMAL
        menu.findItem(R.id.folder_choose).isVisible =
            mode == MultichoiceAdapter.Mode.NORMAL && Global.getUsableFolders(
                this
            ).size > 1
        menu.findItem(R.id.random_favorite).isVisible = mode == MultichoiceAdapter.Mode.NORMAL
        menu.findItem(R.id.delete_all).isVisible = mode == MultichoiceAdapter.Mode.SELECTING
        menu.findItem(R.id.select_all).isVisible = mode == MultichoiceAdapter.Mode.SELECTING
        menu.findItem(R.id.pause_all).isVisible =
            mode == MultichoiceAdapter.Mode.SELECTING && !hasGallery && hasDownloads
        menu.findItem(R.id.start_all).isVisible =
            mode == MultichoiceAdapter.Mode.SELECTING && !hasGallery && hasDownloads
        menu.findItem(R.id.pdf_all).isVisible =
            mode == MultichoiceAdapter.Mode.SELECTING && hasGallery && !hasDownloads && CreatePDF.hasPDFCapabilities()
        menu.findItem(R.id.zip_all).isVisible =
            mode == MultichoiceAdapter.Mode.SELECTING && hasGallery && !hasDownloads
    }

    override fun onDestroy() {
        if (adapter != null) adapter!!.removeObserver()
        super.onDestroy()
    }

    override fun changeLayout(landscape: Boolean) {
        colCount = if (landscape) landscapeColumnCount else portraitColumnCount
        if (adapter != null) adapter!!.setColCount(colCount)
        super.changeLayout(landscape)
    }

    override fun onResume() {
        super.onResume()
        if (idGalleryPosition != -1) {
            adapter!!.updateColor(idGalleryPosition)
            idGalleryPosition = -1
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            return true
        } else if (item.itemId == R.id.pause_all) {
            adapter!!.pauseSelected()
        } else if (item.itemId == R.id.start_all) {
            adapter!!.startSelected()
        } else if (item.itemId == R.id.delete_all) {
            adapter!!.deleteSelected()
        } else if (item.itemId == R.id.pdf_all) {
            adapter!!.pdfSelected()
        } else if (item.itemId == R.id.zip_all) {
            adapter!!.zipSelected()
        } else if (item.itemId == R.id.select_all) {
            adapter!!.selectAll()
        } else if (item.itemId == R.id.folder_choose) {
            showDialogFolderChoose()
        } else if (item.itemId == R.id.random_favorite) {
            if (adapter != null) adapter!!.viewRandom()
        } else if (item.itemId == R.id.sort_by_name) {
            dialogSortType()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showDialogFolderChoose() {
        val strings = Global.getUsableFolders(this)
        val adapter: ArrayAdapter<out File> =
            ArrayAdapter(this, android.R.layout.select_dialog_singlechoice, strings)
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle(R.string.choose_directory).setIcon(R.drawable.ic_folder)
        builder.setAdapter(adapter) { _: DialogInterface?, which: Int ->
            folder = File(strings[which], "NClient")
            FakeInspector(this, folder).execute(this)
        }.setNegativeButton(R.string.cancel, null).show()
    }

    private fun dialogSortType() {
        val sortType = Global.localSortType
        val builder = MaterialAlertDialogBuilder(this)
        val view = LayoutInflater.from(this)
            .inflate(R.layout.local_sort_type, toolbar, false) as LinearLayout
        val group = view.findViewById<ChipGroup>(R.id.chip_group)
        val switchMaterial = view.findViewById<SwitchMaterial>(R.id.ascending)
        group.check(group.getChildAt(sortType.type.ordinal).id)
        switchMaterial.isChecked = sortType.descending
        builder.setView(view)
        builder.setPositiveButton(R.string.ok) { _: DialogInterface?, _: Int ->
            val typeSelectedIndex = group.indexOfChild(group.findViewById(group.checkedChipId))
            val typeSelected = LocalSortType.Type.values()[typeSelectedIndex]
            val descending = switchMaterial.isChecked
            val newSortType = LocalSortType(typeSelected, descending)
            if (sortType == newSortType) return@setPositiveButton
            Global.setLocalSortType(this@LocalActivity, newSortType)
            if (adapter != null) adapter!!.sortChanged()
        }
            .setNeutralButton(R.string.cancel, null)
            .setTitle(R.string.sort_select_type)
            .show()
    }

    override val portraitColumnCount: Int
        get() {
            return Global.colPortDownload
        }

    override val landscapeColumnCount: Int
        get() {
            return Global.colLandDownload
        }

    val query: String
        get() {
            val query = searchView?.query
            return query?.toString() ?: ""
        }
}
