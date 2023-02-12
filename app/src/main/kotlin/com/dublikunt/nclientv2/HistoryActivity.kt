package com.dublikunt.nclientv2

import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.dublikunt.nclientv2.adapters.ListAdapter
import com.dublikunt.nclientv2.api.components.GenericGallery
import com.dublikunt.nclientv2.async.database.Queries
import com.dublikunt.nclientv2.components.activities.BaseActivity
import com.dublikunt.nclientv2.settings.Global
import com.dublikunt.nclientv2.utility.Utility
import com.google.android.material.appbar.MaterialToolbar

class HistoryActivity : BaseActivity() {
    lateinit var adapter: ListAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bookmark)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowTitleEnabled(true)
        supportActionBar!!.setTitle(R.string.history)
        recycler = findViewById(R.id.recycler)
        masterLayout = findViewById(R.id.master_layout)
        adapter = ListAdapter(this)
        adapter.addGalleries(ArrayList<GenericGallery>(Queries.HistoryTable.getHistory()))
        changeLayout(resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
        recycler.adapter = adapter
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        } else if (item.itemId == R.id.cancelAll) {
            Queries.HistoryTable.emptyHistory()
            adapter.restartDataset(ArrayList(1))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun getPortraitColumnCount(): Int {
        return Global.getColPortHistory()
    }

    override fun getLandscapeColumnCount(): Int {
        return Global.getColLandHistory()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.history, menu)
        Utility.tintMenu(menu)
        return true
    }
}
