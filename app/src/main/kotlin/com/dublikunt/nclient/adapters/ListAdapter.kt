package com.dublikunt.nclient.adapters

import android.content.Intent
import android.util.SparseIntArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dublikunt.nclient.GalleryActivity
import com.dublikunt.nclient.MainActivity
import com.dublikunt.nclient.R
import com.dublikunt.nclient.api.Inspector
import com.dublikunt.nclient.api.Inspector.DefaultInspectorResponse
import com.dublikunt.nclient.api.SimpleGallery
import com.dublikunt.nclient.api.components.GenericGallery
import com.dublikunt.nclient.api.enums.Language
import com.dublikunt.nclient.async.database.Queries
import com.dublikunt.nclient.components.activities.BaseActivity
import com.dublikunt.nclient.settings.Global
import com.dublikunt.nclient.settings.Tags
import com.dublikunt.nclient.utility.ImageDownloadUtility.loadImage
import com.dublikunt.nclient.utility.LogUtility.download
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import java.util.*

class ListAdapter(private val context: BaseActivity) :
    RecyclerView.Adapter<GenericAdapter.ViewHolder>() {
    private val statuses = SparseIntArray()
    private val mDataset: MutableList<SimpleGallery?>?
    private val queryString: String?

    init {
        mDataset = ArrayList()
        Global.hasStoragePermission(context)
        queryString = Tags.avoidedTags
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenericAdapter.ViewHolder {
        return GenericAdapter.ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.entry_layout, parent, false)
        )
    }

    private fun loadGallery(holder: GenericAdapter.ViewHolder, ent: SimpleGallery) {
        if (context.isFinishing) return
        try {
            if (Global.isDestroyed(context)) return
            loadImage(context, ent.thumbnail, holder.imgView)
        } catch (ignore: VerifyError) {
        }
    }

    override fun onBindViewHolder(holder: GenericAdapter.ViewHolder, position: Int) {
        val holderPos = holder.bindingAdapterPosition
        if (holderPos >= mDataset!!.size) return
        val ent = mDataset[holderPos] ?: return
        if (!Global.showTitles()) {
            holder.title.alpha = 0f
            holder.flag.alpha = 0f
        } else {
            holder.title.alpha = 1f
            holder.flag.alpha = 1f
        }
        if (context is GalleryActivity) {
            val card = holder.layout.parent as MaterialCardView
            val params = card.layoutParams
            params.width = Global.galleryWidth
            params.height = Global.galleryHeight
            card.layoutParams = params
        }
        holder.overlay.visibility =
            if (queryString != null && ent.hasIgnoredTags(queryString)) View.VISIBLE else View.GONE
        loadGallery(holder, ent)
        holder.pages.visibility = View.GONE
        holder.title.text = ent.title
        holder.flag.visibility = View.VISIBLE
        if (Global.getOnlyLanguage() == Language.ALL || context is GalleryActivity) {
            holder.flag.text = Global.getLanguageFlag(ent.language)
        } else holder.flag.visibility = View.GONE
        holder.title.setOnClickListener {
            val layout = holder.title.layout
            if (layout.getEllipsisCount(layout.lineCount - 1) > 0) holder.title.maxLines =
                7 else if (holder.title.maxLines == 7) holder.title.maxLines =
                3 else holder.layout.performClick()
        }
        holder.layout.setOnClickListener {
            if (context is MainActivity) context.setIdOpenedGallery(ent.id)
            downloadGallery(ent)
            holder.overlay.visibility =
                if (queryString != null && ent.hasIgnoredTags(queryString)) View.VISIBLE else View.GONE
        }
        holder.overlay.setOnClickListener { holder.overlay.visibility = View.GONE }
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

    fun updateColor(id: Int) {
        if (id < 0) return
        var position = -1
        statuses.put(id, Queries.StatusMangaTable.getStatus(id).color)
        for (i in mDataset!!.indices) {
            if (mDataset[i] != null && mDataset[i]!!.id == id) {
                position = id
                break
            }
        }
        if (position >= 0) notifyItemChanged(position)
    }

    private fun downloadGallery(ent: SimpleGallery) {
        Inspector.galleryInspector(context, ent.id, object : DefaultInspectorResponse() {
            override fun onFailure(e: Exception) {
                super.onFailure(e)
                val file = Global.findGalleryFolder(context, ent.id)
                if (file != null) {
                    LocalAdapter.startGallery(context, file)
                } else {
                    context.runOnUiThread {
                        val snackbar = context.masterLayout?.let {
                            Snackbar.make(
                                it,
                                R.string.unable_to_connect_to_the_site,
                                Snackbar.LENGTH_SHORT
                            )
                        }
                        snackbar?.setAction(R.string.retry) { downloadGallery(ent) }
                        snackbar?.show()
                    }
                }
            }

            override fun onSuccess(galleries: List<GenericGallery>) {
                if (galleries.size != 1) {
                    context.runOnUiThread {
                        context.masterLayout?.let {
                            Snackbar.make(
                                it,
                                R.string.no_entry_found,
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }
                    }
                    return
                }
                val intent = Intent(context, GalleryActivity::class.java)
                download(galleries[0].toString())
                intent.putExtra(context.packageName + ".GALLERY", galleries[0])
                context.runOnUiThread { context.startActivity(intent) }
            }
        }).start()
    }

    override fun getItemCount(): Int {
        return mDataset?.size ?: 0
    }

    fun addGalleries(galleries: List<GenericGallery>) {
        val c = mDataset!!.size
        for (g in galleries) {
            mDataset.add(g as SimpleGallery)
            download("Simple: $g")
        }
        download(
            String.format(
                Locale.US,
                "%s,old:%d,new:%d,len%d",
                this,
                c,
                mDataset.size,
                galleries.size
            )
        )
        context.runOnUiThread { notifyItemRangeInserted(c, galleries.size) }
    }

    fun restartDataset(galleries: List<GenericGallery?>) {
        mDataset!!.clear()
        for (g in galleries) if (g is SimpleGallery) mDataset.add(g as SimpleGallery?)
        context.runOnUiThread { notifyDataSetChanged() }
    }

    fun resetStatuses() {
        statuses.clear()
    }
}
