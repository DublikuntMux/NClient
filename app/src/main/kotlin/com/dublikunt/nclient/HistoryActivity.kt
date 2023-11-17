package com.dublikunt.nclient

import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import com.dublikunt.nclient.adapters.ListAdapter
import com.dublikunt.nclient.api.components.GenericGallery
import com.dublikunt.nclient.async.database.Queries
import com.dublikunt.nclient.settings.Global
import com.dublikunt.nclient.utility.Utility

class HistoryActivity : BaseActivity() {
    lateinit var adapter: ListAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Global.initActivity(this);
        setContentView(R.layout.activity_bookmark)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
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

    override val portraitColumnCount: Int
        get() = Global.getColPortHistory()
    override val landscapeColumnCount: Int
        get() = Global.getColLandHistory()

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.history, menu)
        Utility.tintMenu(menu)
        return true
    }
}
