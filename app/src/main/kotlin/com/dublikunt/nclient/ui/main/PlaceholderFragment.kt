package com.dublikunt.nclient.ui.main

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dublikunt.nclient.R
import com.dublikunt.nclient.adapters.StatusViewerAdapter
import com.dublikunt.nclient.components.widgets.CustomGridLayoutManager
import com.dublikunt.nclient.settings.Global

class PlaceholderFragment : Fragment() {
    private lateinit var adapter: StatusViewerAdapter
    private lateinit var recycler: RecyclerView
    private lateinit var refresher: SwipeRefreshLayout
    private fun updateColumnCount(landscape: Boolean) {
        recycler.layoutManager = CustomGridLayoutManager(context, getColumnCount(landscape))
        recycler.adapter = adapter
    }

    private fun getColumnCount(landscape: Boolean): Int {
        return if (landscape) Global.getColLandStatus() else Global.getColPortStatus()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            updateColumnCount(true)
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            updateColumnCount(false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_status_viewer, container, false)
        recycler = root.findViewById(R.id.recycler)
        refresher = root.findViewById(R.id.refresher)
        adapter = StatusViewerAdapter(requireActivity(), requireArguments().getString("STATUS_NAME")!!)
        refresher.setOnRefreshListener {
            adapter.reloadGalleries()
            refresher.isRefreshing = false
        }
        Global.applyFastScroller(recycler)
        updateColumnCount(resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
        return root
    }

    fun changeQuery(newQuery: String?) {
        adapter.setQuery(newQuery)
    }

    fun changeSort(byTitle: Boolean) {
        adapter.updateSort(byTitle)
    }

    fun reload(query: String?, sortByTitle: Boolean) {
        adapter.update(query, sortByTitle)
    }

    companion object {
        fun newInstance(statusName: String?): PlaceholderFragment {
            val fragment = PlaceholderFragment()
            val bundle = Bundle()
            bundle.putString("STATUS_NAME", statusName)
            fragment.arguments = bundle
            return fragment
        }
    }
}
