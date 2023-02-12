package com.dublikunt.nclientv2

import android.os.Bundle
import android.view.MenuItem
import androidx.recyclerview.widget.RecyclerView
import com.dublikunt.nclientv2.adapters.BookmarkAdapter
import com.dublikunt.nclientv2.components.activities.GeneralActivity
import com.dublikunt.nclientv2.components.widgets.CustomLinearLayoutManager
import com.google.android.material.appbar.MaterialToolbar

class BookmarkActivity : GeneralActivity() {
    lateinit var adapter: BookmarkAdapter
    lateinit var recycler: RecyclerView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bookmark)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
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
