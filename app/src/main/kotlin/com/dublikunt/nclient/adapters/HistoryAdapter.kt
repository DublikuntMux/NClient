package com.dublikunt.nclient.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.dublikunt.nclient.R
import com.dublikunt.nclient.SearchActivity
import com.dublikunt.nclient.classes.History
import com.dublikunt.nclient.settings.Global
import com.dublikunt.nclient.utility.ImageDownloadUtility.loadImage

class HistoryAdapter(private val context: SearchActivity) :
    RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {
    private val history: MutableList<History>?
    private var remove = -1

    init {
        if (!Global.isKeepHistory) context.getSharedPreferences("History", 0).edit().clear()
            .apply()
        history = if (Global.isKeepHistory) context.getSharedPreferences("History", 0).getStringSet("history", HashSet())
            ?.let {
                History.setToList(
                    it
                )
            } as MutableList<History>? else null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.entry_history, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        loadImage(
            if (remove == holder.bindingAdapterPosition) R.drawable.ic_close else R.drawable.ic_mode_edit,
            holder.imageButton
        )
        val entry = history!![holder.bindingAdapterPosition].value
        holder.text.text = entry
        holder.master.setOnClickListener { context.setQuery(entry, true) }
        holder.imageButton.setOnLongClickListener {
            context.runOnUiThread {
                if (remove == holder.bindingAdapterPosition) {
                    remove = -1
                    notifyItemChanged(holder.bindingAdapterPosition)
                } else {
                    if (remove != -1) {
                        val l = remove
                        remove = -1
                        notifyItemChanged(l)
                    }
                    remove = holder.bindingAdapterPosition
                    notifyItemChanged(holder.bindingAdapterPosition)
                }
            }
            true
        }
        holder.imageButton.setOnClickListener {
            if (remove == holder.bindingAdapterPosition) {
                removeHistory(remove)
                remove = -1
            } else {
                context.setQuery(entry, false)
            }
        }
    }

    override fun getItemCount(): Int {
        return history?.size ?: 0
    }

    fun addHistory(value: String) {
        if (!Global.isKeepHistory) return
        val history = History(value, false)
        val pos = this.history!!.indexOf(history)
        if (pos >= 0) this.history[pos] = history else this.history.add(history)
        context.getSharedPreferences("History", 0).edit()
            .putStringSet("history", History.listToSet(this.history)).apply()
    }

    fun removeHistory(pos: Int) {
        if (pos < 0 || pos >= history!!.size) return
        history.removeAt(pos)
        context.getSharedPreferences("History", 0).edit().putStringSet(
            "history", History.listToSet(
                history
            )
        ).apply()
        context.runOnUiThread { notifyItemRemoved(pos) }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val master: ConstraintLayout
        val text: TextView
        val imageButton: ImageButton

        init {
            master = itemView.findViewById(R.id.master_layout)
            text = itemView.findViewById(R.id.text)
            imageButton = itemView.findViewById(R.id.edit)
        }
    }
}
