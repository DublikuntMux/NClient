package com.dublikunt.nclientv2.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dublikunt.nclientv2.R
import com.dublikunt.nclientv2.api.components.GenericGallery

abstract class GenericAdapter<T : GenericGallery?> internal constructor(val dataset: List<T>) :
    RecyclerView.Adapter<GenericAdapter.ViewHolder>(), Filterable {
    var filter: List<T>
    var lastQuery = ""

    init {
        dataset.sortedBy { obj: T -> obj!!.title }
        filter = ArrayList(dataset)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.entry_layout, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return filter.size
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence): FilterResults? {
                val query = constraint.toString().lowercase()
                if (lastQuery == query) return null
                val results = FilterResults()
                results.count = filter.size
                lastQuery = query
                val filter: MutableList<T> = ArrayList()
                for (gallery in dataset) if (gallery!!.title.lowercase()
                        .contains(query)
                ) filter.add(gallery)
                results.values = filter
                return results
            }

            override fun publishResults(constraint: CharSequence, results: FilterResults) {
                filter = results.values as List<T>
                if (filter.size > results.count) notifyItemRangeInserted(
                    results.count,
                    filter.size - results.count
                ) else if (filter.size < results.count) notifyItemRangeRemoved(
                    filter.size,
                    results.count - filter.size
                )
                notifyItemRangeRemoved(filter.size, results.count)
                notifyItemRangeChanged(0, filter.size - 1)
            }
        }
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        @JvmField
        val imgView: ImageView
        @JvmField
        val overlay: View
        @JvmField
        val title: TextView
        @JvmField
        val pages: TextView
        @JvmField
        val flag: TextView
        @JvmField
        val layout: View

        init {
            imgView = v.findViewById(R.id.image)
            title = v.findViewById(R.id.title)
            pages = v.findViewById(R.id.pages)
            layout = v.findViewById(R.id.master_layout)
            flag = v.findViewById(R.id.flag)
            overlay = v.findViewById(R.id.overlay)
        }
    }
}
