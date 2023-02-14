package com.dublikunt.nclient.components.activities

import android.content.res.Configuration
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dublikunt.nclient.components.widgets.CustomGridLayoutManager

abstract class BaseActivity : GeneralActivity() {
    lateinit var recycler: RecyclerView
        protected set
    lateinit var refresher: SwipeRefreshLayout
        protected set
    var masterLayout: ViewGroup? = null
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
        val manager = recycler.layoutManager as CustomGridLayoutManager?
        val adapter = recycler.adapter
        val count = if (landscape) landscapeColumnCount else portraitColumnCount
        var position = 0
        if (manager != null) position = manager.findFirstCompletelyVisibleItemPosition()
        val gridLayoutManager = CustomGridLayoutManager(this, count)
        recycler.layoutManager = gridLayoutManager
        recycler.adapter = adapter
        recycler.scrollToPosition(position)
    }
}
