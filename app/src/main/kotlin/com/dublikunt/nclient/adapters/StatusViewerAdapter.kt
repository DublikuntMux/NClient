package com.dublikunt.nclient.adapters

import android.content.Intent
import android.database.Cursor
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.dublikunt.nclient.GalleryActivity
import com.dublikunt.nclient.R
import com.dublikunt.nclient.api.components.Gallery
import com.dublikunt.nclient.async.database.Queries
import com.dublikunt.nclient.settings.Global
import com.dublikunt.nclient.utility.ImageDownloadUtility.loadImage
import com.dublikunt.nclient.utility.LogUtility.download
import java.io.IOException
import java.util.*

class StatusViewerAdapter(private val context: AppCompatActivity, private val statusName: String) :
    RecyclerView.Adapter<GenericAdapter.ViewHolder>() {
    private var query = ""
    private var sortByTitle = false
    private var galleries: Cursor? = null

    init {
        reloadGalleries()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenericAdapter.ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.entry_layout, parent, false)
        return GenericAdapter.ViewHolder(view)
    }

    override fun onBindViewHolder(holder: GenericAdapter.ViewHolder, position: Int) {
        val ent = positionToGallery(holder.bindingAdapterPosition) ?: return
        loadImage(context, ent.thumbnail, holder.imgView)
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
            val intent = Intent(context, GalleryActivity::class.java)
            download(ent.toString() + "")
            intent.putExtra(context.packageName + ".GALLERY", ent)
            context.startActivity(intent)
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
    }

    private fun positionToGallery(position: Int): Gallery? {
        try {
            if (galleries != null && galleries!!.moveToPosition(position)) {
                return Queries.GalleryTable.cursorToGallery(galleries)
            }
        } catch (ignore: IOException) {
        }
        return null
    }

    override fun getItemCount(): Int {
        return if (galleries != null) galleries!!.count else 0
    }

    fun setGalleries(galleries: Cursor?) {
        if (this.galleries != null) this.galleries!!.close()
        this.galleries = galleries
        context.runOnUiThread { notifyDataSetChanged() }
    }

    fun reloadGalleries() {
        setGalleries(Queries.StatusMangaTable.getGalleryOfStatus(statusName, query, sortByTitle))
    }

    fun setQuery(newQuery: String?) {
        query = newQuery ?: ""
        reloadGalleries()
    }

    fun updateSort(byTitle: Boolean) {
        sortByTitle = byTitle
        reloadGalleries()
    }

    fun update(newQuery: String?, byTitle: Boolean) {
        if (query == newQuery && byTitle == sortByTitle) return
        query = newQuery ?: ""
        sortByTitle = byTitle
        reloadGalleries()
    }
}
