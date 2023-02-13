package com.dublikunt.nclientv2.components.widgets

import android.content.Context
import android.util.AttributeSet
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dublikunt.nclientv2.utility.LogUtility
import com.dublikunt.nclientv2.utility.LogUtility.error

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
