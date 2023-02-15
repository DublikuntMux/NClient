package com.dublikunt.nclient.adapters

import android.content.DialogInterface
import android.database.Cursor
import android.util.JsonWriter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.dublikunt.nclient.R
import com.dublikunt.nclient.TagFilterActivity
import com.dublikunt.nclient.api.components.Tag
import com.dublikunt.nclient.api.enums.TagStatus
import com.dublikunt.nclient.api.enums.TagType
import com.dublikunt.nclient.async.database.Queries
import com.dublikunt.nclient.settings.AuthRequest
import com.dublikunt.nclient.settings.Global.setTint
import com.dublikunt.nclient.settings.Login
import com.dublikunt.nclient.settings.TagV2
import com.dublikunt.nclient.utility.ImageDownloadUtility.loadImage
import com.dublikunt.nclient.utility.LogUtility.download
import com.dublikunt.nclient.utility.Utility
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.io.StringWriter
import java.util.*

class TagsAdapter : RecyclerView.Adapter<TagsAdapter.ViewHolder>, Filterable {
    private val context: TagFilterActivity
    private val logged = Login.isLogged()
    private val type: TagType?
    private val tagMode: TagMode
    private var lastQuery: String? = null
    private var cursor: Cursor? = null

    constructor(cont: TagFilterActivity, query: String?, online: Boolean) {
        context = cont
        type = null
        tagMode = if (online) TagMode.ONLINE else TagMode.OFFLINE
        filter.filter(query)
    }

    constructor(cont: TagFilterActivity, query: String?, type: TagType?) {
        context = cont
        this.type = type
        tagMode = TagMode.TYPE
        filter.filter(query)
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence): FilterResults {
                var constraint: CharSequence? = constraint
                val results = FilterResults()
                if (constraint == null) constraint = ""
                lastQuery = constraint.toString()
                val tags = Queries.TagTable.getFilterCursor(
                    lastQuery!!,
                    type,
                    tagMode == TagMode.ONLINE,
                    TagV2.isSortedByName
                )
                results.count = tags.count
                results.values = tags
                download(results.count.toString() + "," + results.values)
                return results
            }

            override fun publishResults(constraint: CharSequence, results: FilterResults) {
                if (results.count == -1) return
                val newCursor = results.values as Cursor
                val oldCount = itemCount
                val newCount = results.count
                if (cursor != null) cursor!!.close()
                cursor = newCursor
                if (newCount > oldCount) notifyItemRangeInserted(
                    oldCount,
                    newCount - oldCount
                ) else notifyItemRangeRemoved(newCount, oldCount - newCount)
                notifyItemRangeChanged(0, Math.min(newCount, oldCount))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.entry_tag_layout, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        cursor!!.moveToPosition(position)
        val ent = Queries.TagTable.cursorToTag(cursor!!)
        holder.title.text = ent.name
        holder.count.text = String.format(Locale.US, "%d", ent.count)
        holder.master.setOnClickListener { v: View? ->
            when (tagMode) {
                TagMode.OFFLINE, TagMode.TYPE -> if (TagV2.maxTagReached() && ent.status == TagStatus.DEFAULT) {
                    context.runOnUiThread {
                        Toast.makeText(
                            context,
                            context.getString(R.string.tags_max_reached, TagV2.MAXTAGS),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    TagV2.updateStatus(ent)
                    updateLogo(holder.imgView, ent.status)
                }
                TagMode.ONLINE -> try {
                    onlineTagUpdate(ent, !Login.isOnlineTags(ent), holder.imgView)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        if (tagMode != TagMode.ONLINE && logged) holder.master.setOnLongClickListener { view: View? ->
            if (!Login.isOnlineTags(ent)) showBlacklistDialog(
                ent,
                holder.imgView
            ) else Toast.makeText(context, R.string.tag_already_in_blacklist, Toast.LENGTH_SHORT)
                .show()
            true
        }
        updateLogo(holder.imgView, if (tagMode == TagMode.ONLINE) TagStatus.AVOIDED else ent.status)
    }

    override fun getItemCount(): Int {
        return if (cursor == null) 0 else cursor!!.count
    }

    private fun showBlacklistDialog(tag: Tag, imgView: ImageView) {
        val builder = MaterialAlertDialogBuilder(context)
        builder.setIcon(R.drawable.ic_star_border).setTitle(R.string.add_to_online_blacklist)
            .setMessage(R.string.are_you_sure)
        builder.setPositiveButton(R.string.yes) { dialogInterface: DialogInterface?, i: Int ->
            try {
                onlineTagUpdate(tag, true, imgView)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.setNegativeButton(R.string.no, null).show()
    }

    @Throws(IOException::class)
    private fun onlineTagUpdate(tag: Tag, add: Boolean, imgView: ImageView) {
        if (!Login.isLogged() || Login.user == null) return
        val sw = StringWriter()
        val jw = JsonWriter(sw)
        jw.beginObject().name("added").beginArray()
        if (add) writeTag(jw, tag)
        jw.endArray().name("removed").beginArray()
        if (!add) writeTag(jw, tag)
        jw.endArray().endObject()
        val url = String.format(
            Locale.US,
            Utility.getBaseUrl() + "users/%d/%s/blacklist",
            Login.user!!.id,
            Login.user!!.codename
        )
        val ss: RequestBody = sw.toString().toRequestBody("application/json".toMediaType())
        AuthRequest(url, url, object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                if (response.body.string().contains("ok")) {
                    if (add) Login.addOnlineTag(tag) else Login.removeOnlineTag(tag)
                    if (tagMode == TagMode.ONLINE) updateLogo(
                        imgView,
                        if (add) TagStatus.AVOIDED else TagStatus.DEFAULT
                    )
                }
            }
        }).setMethod("POST", ss).start()
    }

    private fun updateLogo(img: ImageView, s: TagStatus) {
        context.runOnUiThread {
            when (s) {
                TagStatus.DEFAULT -> img.setImageDrawable(null)
                TagStatus.ACCEPTED -> {
                    loadImage(R.drawable.ic_check, img)
                    setTint(img.drawable)
                }
                TagStatus.AVOIDED -> {
                    loadImage(R.drawable.ic_close, img)
                    setTint(img.drawable)
                }
            }
        }
    }

    fun addItem() {
        filter.filter(lastQuery)
    }

    private enum class TagMode {
        ONLINE, OFFLINE, TYPE
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val imgView: ImageView
        val title: TextView
        val count: TextView
        val master: ConstraintLayout

        init {
            imgView = v.findViewById(R.id.image)
            title = v.findViewById(R.id.title)
            count = v.findViewById(R.id.count)
            master = v.findViewById(R.id.master_layout)
        }
    }

    companion object {
        @Throws(IOException::class)
        private fun writeTag(jw: JsonWriter, tag: Tag) {
            jw.beginObject()
            jw.name("id").value(tag.id.toLong())
            jw.name("name").value(tag.name)
            jw.name("type").value(tag.typeSingleName)
            jw.endObject()
        }
    }
}
