package com.dublikunt.nclient.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.dublikunt.nclient.BookmarkActivity
import com.dublikunt.nclient.MainActivity
import com.dublikunt.nclient.R
import com.dublikunt.nclient.async.database.Queries
import com.dublikunt.nclient.classes.Bookmark
import com.dublikunt.nclient.utility.IntentUtility.startAnotherActivity
import com.google.android.material.button.MaterialButton

class BookmarkAdapter(private val bookmarkActivity: BookmarkActivity) :
    RecyclerView.Adapter<BookmarkAdapter.ViewHolder>() {
    private val bookmarks: MutableList<Bookmark> =
        Queries.BookmarkTable.bookmarks as MutableList<Bookmark>

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(LAYOUT, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, pos: Int) {
        val position = holder.bindingAdapterPosition
        val bookmark = bookmarks[position]
        holder.queryText.text = bookmark.toString()
        holder.pageLabel.text =
            bookmarkActivity.getString(R.string.bookmark_page_format, bookmark.page)
        holder.deleteButton.setOnClickListener { removeBookmarkAtPosition(position) }
        holder.rootLayout.setOnClickListener { loadBookmark(bookmark) }
    }

    private fun loadBookmark(bookmark: Bookmark) {
        val i = Intent(bookmarkActivity, MainActivity::class.java)
        i.putExtra(bookmarkActivity.packageName + ".BYBOOKMARK", true)
        i.putExtra(
            bookmarkActivity.packageName + ".INSPECTOR", bookmark.createInspector(
                bookmarkActivity, null
            )
        )
        startAnotherActivity(bookmarkActivity, i)
    }

    private fun removeBookmarkAtPosition(position: Int) {
        if (position >= bookmarks.size) return
        val bookmark = bookmarks[position]
        bookmark.deleteBookmark()
        bookmarks.remove(bookmark)
        bookmarkActivity.runOnUiThread { notifyItemRemoved(position) }
    }

    override fun getItemCount(): Int {
        return bookmarks.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deleteButton: MaterialButton
        val queryText: TextView
        val pageLabel: TextView
        val rootLayout: ConstraintLayout

        init {
            deleteButton = itemView.findViewById(R.id.remove_button)
            pageLabel = itemView.findViewById(R.id.page)
            queryText = itemView.findViewById(R.id.title)
            rootLayout = itemView.findViewById(R.id.master_layout)
        }
    }

    companion object {
        private const val LAYOUT = R.layout.bookmark_layout
    }
}
