package com.dublikunt.nclient.components.widgets

import android.content.Context
import android.util.AttributeSet
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dublikunt.nclient.utility.LogUtility.error

class CustomSwipe : SwipeRefreshLayout {
    constructor(context: Context) : super(context) {}
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {}

    override fun setEnabled(refreshing: Boolean) {
        try {
            throw Exception()
        } catch (e: Exception) {
           error("NEW VALUE: " + refreshing + ",," + e.localizedMessage)
        }
        super.setRefreshing(refreshing)
    }
}
