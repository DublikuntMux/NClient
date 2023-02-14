package com.dublikunt.nclient.components.widgets

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager
import kotlin.math.max

class CustomGridLayoutManager : GridLayoutManager {
    constructor(context: Context?, spanCount: Int) : super(context, max(1, spanCount)) {}
    constructor(
        context: Context?,
        spanCount: Int,
        orientation: Int,
        reverseLayout: Boolean
    ) : super(context, max(1, spanCount), orientation, reverseLayout) {
    }

    override fun supportsPredictiveItemAnimations(): Boolean {
        return false
    }
}
