package com.dublikunt.nclientv2.components.activities

import android.content.res.Configuration
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dublikunt.nclientv2.components.widgets.CustomGridLayoutManager

abstract class BaseActivity : GeneralActivity() {
    lateinit var recycler: RecyclerView
        protected set
    lateinit var refresher: SwipeRefreshLayout
        protected set
    lateinit var masterLayout: ViewGroup
        protected set
    protected abstract val portraitColumnCount: Int
    protected abstract val landscapeColumnCount: Int
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            changeLayout(true)
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            changeLayout(false)
        }
    }

    protected open fun changeLayout(landscape: Boolean) {
        val manager = recycler!!.layoutManager as CustomGridLayoutManager?
        val adapter = recycler!!.adapter
        val count = if (landscape) landscapeColumnCount else portraitColumnCount
        var position = 0
        if (manager != null) position = manager.findFirstCompletelyVisibleItemPosition()
        val gridLayoutManager = CustomGridLayoutManager(this, count)
        recycler!!.layoutManager = gridLayoutManager
        recycler!!.adapter = adapter
        recycler!!.scrollToPosition(position)
    }
}