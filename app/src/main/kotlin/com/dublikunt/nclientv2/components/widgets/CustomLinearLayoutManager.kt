package com.dublikunt.nclientv2.components.widgets

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager

class CustomLinearLayoutManager(context: Context?) : LinearLayoutManager(context) {
    override fun supportsPredictiveItemAnimations(): Boolean {
        return false
    }
}
