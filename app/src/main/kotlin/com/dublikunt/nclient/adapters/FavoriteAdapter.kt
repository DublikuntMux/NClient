package com.dublikunt.nclient.adapters

import android.annotation.SuppressLint
import android.content.Intent
import android.database.Cursor
import android.util.SparseIntArray
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.dublikunt.nclient.FavoriteActivity
import com.dublikunt.nclient.FavoriteActivity.Companion.entryPerPage
import com.dublikunt.nclient.GalleryActivity
import com.dublikunt.nclient.R
import com.dublikunt.nclient.api.components.Gallery
import com.dublikunt.nclient.async.database.Queries
import com.dublikunt.nclient.settings.Global
import com.dublikunt.nclient.utility.ImageDownloadUtility.loadImage
import com.dublikunt.nclient.utility.LogUtility.download
import com.dublikunt.nclient.utility.LogUtility.error
import com.dublikunt.nclient.utility.Utility
import java.io.IOException
import java.util.*

class FavoriteAdapter(private val activity: FavoriteActivity) :
    RecyclerView.Adapter<GenericAdapter.ViewHolder>(), Filterable {
    private val perPage = entryPerPage
    private val statuses = SparseIntArray()
    private lateinit var galleries: Array<Gallery?>
    private var lastQuery: CharSequence = ""
    private var cursor: Cursor? = null
    private var force = false
    private var sortByTitle = false

    init {
        setHasStableIds(true)
    }

    @SuppressLint("Range")
    override fun getItemId(position: Int): Long {
        cursor!!.moveToPosition(position)
        return cursor!!.getInt(cursor!!.getColumnIndex(Queries.GalleryTable.IDGALLERY)).toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenericAdapter.ViewHolder {
        return GenericAdapter.ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.entry_layout, parent, false)
        )
    }

    private fun galleryFromPosition(position: Int): Gallery? {
        if (galleries[position] != null) return galleries[position]
        cursor!!.moveToPosition(position)
        return try {
            val g = Queries.GalleryTable.cursorToGallery(cursor!!)
            galleries[position] = g
            g
        } catch (e: IOException) {
            null
        }
    }

    override fun onBindViewHolder(holder: GenericAdapter.ViewHolder, position: Int) {
        val ent = galleryFromPosition(holder.bindingAdapterPosition) ?: return
        loadImage(activity, ent.thumbnail, holder.imgView)
        holder.pages.text = String.format(Locale.US, "%d", ent.pageCount)
        holder.title.text = ent.title
        holder.flag.text = Global.getLanguageFlag(ent.language)
        holder.title.setOnClickListener {
            val layout = holder.title.layout
            if (layout.getEllipsisCount(layout.lineCount - 1) > 0) holder.title.maxLines =
                7 else if (holder.title.maxLines == 7) holder.title.maxLines =
                3 else holder.layout.performClick()
        }
        holder.layout.setOnClickListener {
            startGallery(ent)
        }
        holder.layout.setOnLongClickListener {
            holder.title.animate().alpha(if (holder.title.alpha == 0f) 1f else 0f).setDuration(100)
                .start()
            holder.flag.animate().alpha(if (holder.flag.alpha == 0f) 1f else 0f).setDuration(100)
                .start()
            holder.pages.animate().alpha(if (holder.pages.alpha == 0f) 1f else 0f).setDuration(100)
                .start()
            true
        }
        var statusColor = statuses[ent.id, 0]
        if (statusColor == 0) {
            statusColor = Queries.StatusMangaTable.getStatus(ent.id).color
            statuses.put(ent.id, statusColor)
        }
        holder.title.setBackgroundColor(statusColor)
    }

    private fun startGallery(ent: Gallery?) {
        val intent = Intent(activity, GalleryActivity::class.java)
        download(ent.toString() + "")
        intent.putExtra(activity.packageName + ".GALLERY", ent)
        intent.putExtra(activity.packageName + ".UNKNOWN", true)
        activity.startActivity(intent)
    }

    fun changePage() {
        forceReload()
    }

    fun updateColor(position: Int) {
        val ent = galleryFromPosition(position) ?: return
        val id = ent.id
        statuses.put(id, Queries.StatusMangaTable.getStatus(id).color)
        notifyItemChanged(position)
    }

    override fun getItemCount(): Int {
        return if (cursor == null) 0 else cursor!!.count
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence): FilterResults? {
                var constraint = constraint
                constraint = constraint.toString().lowercase()
                if (!force && lastQuery == constraint) return null
                download("FILTERING")
                setRefresh(true)
                val results = FilterResults()
                lastQuery = constraint.toString()
                download(lastQuery.toString() + "LASTQERY")
                force = false
                val c: Cursor = Queries.FavoriteTable.getAllFavoriteGalleriesCursor(
                    lastQuery,
                    sortByTitle,
                    perPage,
                    (activity.getActualPage() - 1) * perPage
                )
                results.count = c.count
                results.values = c
                download("FILTERING3")
                error(results.count.toString() + ";" + results.values)
                setRefresh(false)
                return results
            }

            override fun publishResults(constraint: CharSequence, results: FilterResults) {
                setRefresh(true)
                download("After called2")
                val oldSize = itemCount
                val newSize = results.count
                updateCursor(results.values as Cursor)
                //not in runOnUIThread because is always executed on UI
                if (oldSize > newSize) notifyItemRangeRemoved(
                    newSize,
                    oldSize - newSize
                ) else notifyItemRangeInserted(oldSize, newSize - oldSize)
                notifyItemRangeChanged(0, Math.min(newSize, oldSize))
                setRefresh(false)
            }
        }
    }

    fun setSortByTitle(sortByTitle: Boolean) {
        this.sortByTitle = sortByTitle
        forceReload()
    }

    fun forceReload() {
        force = true
        activity.runOnUiThread { filter.filter(lastQuery) }
    }

    fun setRefresh(refresh: Boolean) {
        activity.runOnUiThread { activity.refresher.isRefreshing = refresh }
    }

    fun clearGalleries() {
        Queries.FavoriteTable.removeAllFavorite()
        val s = itemCount
        updateCursor(null)
        activity.runOnUiThread { notifyItemRangeRemoved(0, s) }
    }

    private fun updateCursor(c: Cursor?) {
        if (cursor != null) cursor!!.close()
        galleries = arrayOfNulls(c?.count ?: 0)
        cursor = c
        statuses.clear()
    }

    val allGalleries: Collection<Gallery?>
        get() {
            if (cursor == null) return emptyList<Gallery>()
            val count = cursor!!.count
            val galleries = ArrayList<Gallery?>(count)
            for (i in 0 until count) galleries.add(galleryFromPosition(i))
            return galleries
        }

    fun randomGallery() {
        if (cursor == null || cursor!!.count < 1) return
        startGallery(
            galleryFromPosition(
                Utility.RANDOM.nextInt(
                    cursor!!.count
                )
            )
        )
    }
}
