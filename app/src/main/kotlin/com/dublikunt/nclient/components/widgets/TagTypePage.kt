package com.dublikunt.nclient.components.widgets

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.dublikunt.nclient.R
import com.dublikunt.nclient.TagFilterActivity
import com.dublikunt.nclient.adapters.TagsAdapter
import com.dublikunt.nclient.async.ScrapeTags
import com.dublikunt.nclient.enums.TagType
import com.dublikunt.nclient.settings.Global
import com.dublikunt.nclient.settings.Tags

class TagTypePage : Fragment() {
    private lateinit var type: TagType
    var recyclerView: RecyclerView? = null
        private set
    private var activity: TagFilterActivity? = null
    private var query: String? = null
    private var adapter: TagsAdapter? = null
    fun setQuery(query: String) {
        this.query = query
        refilter(query)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        activity = getActivity() as TagFilterActivity?
        type = TagType.values[requireArguments().getInt("TAGTYPE")]
        val rootView = inflater.inflate(R.layout.fragment_tag_filter, container, false)
        recyclerView = rootView.findViewById(R.id.recycler)
        Global.applyFastScroller(recyclerView)
        loadTags()
        return rootView
    }

    private fun loadTags() {
        recyclerView!!.layoutManager = CustomGridLayoutManager(
            activity,
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 4 else 2
        )
        adapter = when (type) {
            TagType.UNKNOWN -> activity?.let {
                TagsAdapter(
                    it,
                    query,
                    false
                )
            }
            TagType.CATEGORY -> activity?.let { TagsAdapter(it, query, true) }
            else -> activity?.let {
                TagsAdapter(
                    it,
                    query,
                    type
                )
            }
        }
        recyclerView!!.adapter = adapter
    }

    fun refilter(newText: String?) {
        if (activity != null) requireActivity().runOnUiThread { adapter!!.filter.filter(newText) }
    }

    fun reset() {
        if (type == TagType.UNKNOWN) Tags.resetAllStatus() else if (type != TagType.CATEGORY) {
            ScrapeTags.startWork(activity)
        }
        val activity = getActivity() as AppCompatActivity?
        if (activity == null || adapter == null) return
        activity.runOnUiThread(Runnable { adapter!!.notifyDataSetChanged() })
    }

    fun changeSize() {
        refilter(query)
    }

    companion object {
        private fun getTag(page: Int): Int {
            when (page) {
                0 -> return TagType.UNKNOWN.id.toInt() //tags with status
                1 -> return TagType.TAG.id.toInt()
                2 -> return TagType.ARTIST.id.toInt()
                3 -> return TagType.CHARACTER.id.toInt()
                4 -> return TagType.PARODY.id.toInt()
                5 -> return TagType.GROUP.id.toInt()
                6 -> return TagType.CATEGORY.id.toInt() //online blacklisted tags
            }
            return -1
        }

        fun newInstance(page: Int): TagTypePage {
            val fragment = TagTypePage()
            val args = Bundle()
            args.putInt("TAGTYPE", getTag(page))
            fragment.arguments = args
            return fragment
        }
    }
}
