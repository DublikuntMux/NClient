package com.dublikunt.nclientv2

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.dublikunt.nclientv2.components.activities.GeneralActivity
import com.dublikunt.nclientv2.settings.Global
import com.dublikunt.nclientv2.ui.main.PlaceholderFragment
import com.dublikunt.nclientv2.ui.main.SectionsPagerAdapter
import com.dublikunt.nclientv2.utility.LogUtility
import com.dublikunt.nclientv2.utility.Utility
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class StatusViewerActivity : GeneralActivity() {
    private var sortByTitle = false
    private var query: String? = null
    private lateinit var viewPager: ViewPager2
    private lateinit var sectionsPagerAdapter: SectionsPagerAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_status_viewer)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowTitleEnabled(true)
        supportActionBar!!.setTitle(R.string.manage_statuses)
        viewPager = findViewById(R.id.view_pager)
        sectionsPagerAdapter = SectionsPagerAdapter(this)
        viewPager.adapter = sectionsPagerAdapter
        viewPager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val fragment = getPositionFragment(position)
                fragment?.reload(query, sortByTitle)
            }
        })
        val tabs = findViewById<TabLayout>(R.id.tabs)
        TabLayoutMediator(
            tabs,
            viewPager
        ) { tab: TabLayout.Tab, position: Int ->
            tab.text = sectionsPagerAdapter.getPageTitle(position)
        }.attach()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        } else if (item.itemId == R.id.sort_by_name) {
            sortByTitle = !sortByTitle
            val fragment = actualFragment
            fragment?.changeSort(sortByTitle)
            item.setTitle(if (sortByTitle) R.string.sort_by_latest else R.string.sort_by_title)
            item.setIcon(if (sortByTitle) R.drawable.ic_sort_by_alpha else R.drawable.ic_access_time)
            Global.setTint(item.icon)
        }
        return super.onOptionsItemSelected(item)
    }

    private val actualFragment: PlaceholderFragment?
        get() = getPositionFragment(viewPager.currentItem)

    private fun getPositionFragment(position: Int): PlaceholderFragment? {
        val f = supportFragmentManager.findFragmentByTag("f$position") as PlaceholderFragment?
        if (f != null) {
            LogUtility.download(f)
        }
        return f
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.status_viewer, menu)
        val searchView = menu.findItem(R.id.search).actionView as SearchView?
        searchView!!.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                query = newText
                actualFragment?.changeQuery(query)
                return true
            }
        })
        Utility.tintMenu(menu)
        return super.onCreateOptionsMenu(menu)
    }
}
