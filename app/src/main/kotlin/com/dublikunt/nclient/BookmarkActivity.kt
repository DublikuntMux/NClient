package com.dublikunt.nclient

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import com.dublikunt.nclient.adapters.BookmarkAdapter
import com.dublikunt.nclient.components.widgets.CustomLinearLayoutManager

class BookmarkActivity : GeneralActivity() {
    lateinit var adapter: BookmarkAdapter
    lateinit var recycler: RecyclerView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Global.initActivity(this);
        setContentView(R.layout.activity_bookmark)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowTitleEnabled(true)
        supportActionBar!!.setTitle(R.string.manage_bookmarks)
        recycler = findViewById(R.id.recycler)
        adapter = BookmarkAdapter(this)
        recycler.layoutManager = CustomLinearLayoutManager(this)
        recycler.adapter = adapter
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
