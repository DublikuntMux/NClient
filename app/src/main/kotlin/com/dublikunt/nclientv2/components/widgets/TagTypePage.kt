package com.dublikunt.nclientv2.components.widgets

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.dublikunt.nclientv2.R
import com.dublikunt.nclientv2.TagFilterActivity
import com.dublikunt.nclientv2.adapters.TagsAdapter
import com.dublikunt.nclientv2.api.enums.TagType
import com.dublikunt.nclientv2.async.ScrapeTags
import com.dublikunt.nclientv2.settings.Global
import com.dublikunt.nclientv2.settings.TagV2

class TagTypePage : Fragment() {
    private var type: TagType? = null
    var recyclerView: RecyclerView? = null
        private set
    private var activity: TagFilterActivity? = null
    private var query: String? = null
    private var adapter: TagsAdapter? = null
    fun setQuery(query: String?) {
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

    fun loadTags() {
        recyclerView!!.layoutManager = CustomGridLayoutManager(
            activity,
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 4 else 2
        )
        adapter = if (type == TagType.UNKNOWN) TagsAdapter(
            activity,
            query,
            false
        ) else if (type == TagType.CATEGORY) TagsAdapter(activity, query, true) else TagsAdapter(
            activity,
            query,
            type
        )
        recyclerView!!.adapter = adapter
    }

    fun refilter(newText: String?) {
        if (activity != null) requireActivity().runOnUiThread { adapter!!.filter.filter(newText) }
    }

    fun reset() {
        if (type == TagType.UNKNOWN) TagV2.resetAllStatus() else if (type != TagType.CATEGORY) {
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
