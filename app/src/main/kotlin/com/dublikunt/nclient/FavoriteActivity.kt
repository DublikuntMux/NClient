package com.dublikunt.nclient

import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import com.dublikunt.nclient.adapters.FavoriteAdapter
import com.dublikunt.nclient.async.database.Queries
import com.dublikunt.nclient.async.downloader.DownloadGallery
import com.dublikunt.nclient.components.activities.BaseActivity
import com.dublikunt.nclient.components.views.PageSwitcher
import com.dublikunt.nclient.settings.Global
import com.dublikunt.nclient.utility.Utility
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class FavoriteActivity : BaseActivity() {
    private lateinit var adapter: FavoriteAdapter
    private var sortByTitle = false
    private lateinit var pageSwitcher: PageSwitcher
    private var searchView: SearchView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.app_bar_main)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowTitleEnabled(true)
        supportActionBar!!.setTitle(R.string.favorite_manga)
        recycler = findViewById(R.id.recycler)
        refresher = findViewById(R.id.refresher)
        pageSwitcher = findViewById(R.id.page_switcher)
        refresher.isRefreshing = true
        adapter = FavoriteAdapter(this)
        refresher.setOnRefreshListener { adapter.forceReload() }
        changeLayout(resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
        recycler.adapter = adapter
        pageSwitcher.setChanger(object : PageSwitcher.DefaultPageChanger() {
            override fun pageChanged(switcher: PageSwitcher, page: Int) {
                adapter.changePage()
            }
        })
        pageSwitcher.setPages(1, 1)
    }

    fun getActualPage(): Int {
        return pageSwitcher.getActualPage()
    }

    fun changePages(totalPages: Int, actualPages: Int) {
        pageSwitcher.setPages(totalPages, actualPages)
    }

    override val portraitColumnCount: Int
        get() {
            return Global.colPortFavorite
        }

    override val landscapeColumnCount: Int
        get() {
            return Global.colLandFavorite
        }

    private fun calculatePages(text: String?): Int {
        val perPage = entryPerPage
        val totalEntries = Queries.FavoriteTable.countFavorite(text)
        val div = totalEntries / perPage
        val mod = totalEntries % perPage
        return div + if (mod == 0) 0 else 1
    }

    override fun onResume() {
        refresher.isEnabled = true
        refresher.isRefreshing = true
        val query = searchView?.query.toString()
        pageSwitcher.setTotalPage(calculatePages(query))
        adapter.forceReload()
        super.onResume()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        menu.findItem(R.id.download_page).isVisible = true
        menu.findItem(R.id.sort_by_name).isVisible = true
        menu.findItem(R.id.by_popular).isVisible = false
        menu.findItem(R.id.only_language).isVisible = false
        menu.findItem(R.id.add_bookmark).isVisible = false
        searchView = (menu.findItem(R.id.search).actionView as SearchView?)!!
        searchView!!.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                pageSwitcher.setTotalPage(calculatePages(newText))
                adapter.filter.filter(newText)
                return true
            }
        })
        Utility.tintMenu(menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val i: Intent
        when (item.itemId) {
            R.id.open_browser -> {
                i = Intent(Intent.ACTION_VIEW, Uri.parse(Utility.getBaseUrl() + "favorites/"))
                startActivity(i)
            }
            R.id.download_page -> {
                showDialogDownloadAll()
            }
            R.id.sort_by_name -> {
                sortByTitle = !sortByTitle
                adapter.setSortByTitle(sortByTitle)
                item.setTitle(if (sortByTitle) R.string.sort_by_latest else R.string.sort_by_title)
            }
            R.id.random_favorite -> {
                adapter.randomGallery()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showDialogDownloadAll() {
        val builder = MaterialAlertDialogBuilder(this)
        builder
            .setTitle(R.string.download_all_galleries_in_this_page)
            .setIcon(R.drawable.ic_file)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok) { _: DialogInterface?, _: Int ->
                for (g in adapter.allGalleries) g?.let {
                    DownloadGallery.downloadGallery(
                        this,
                        it
                    )
                }
            }
        builder.show()
    }

    companion object {
        @JvmStatic
        val entryPerPage: Int
            get() = Int.MAX_VALUE
    }
}
