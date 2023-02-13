package com.dublikunt.nclientv2.components.widgets

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

class CustomImageView : AppCompatImageView {
    constructor(context: Context?) : super(context!!)
    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!, attrs
    )

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context!!, attrs, defStyleAttr
    )

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        invalidate()
    }

    override fun setFrame(l: Int, t: Int, r: Int, b: Int): Boolean {
        val matrix = imageMatrix
        val scale: Float
        val viewWidth = width - paddingLeft - paddingRight
        val viewHeight = height - paddingTop - paddingBottom
        val drawableWidth = drawable.intrinsicWidth
        val drawableHeight = drawable.intrinsicHeight
        scale = if (drawableWidth * viewHeight > drawableHeight * viewWidth) {
            viewHeight.toFloat() / drawableHeight.toFloat()
        } else {
            viewWidth.toFloat() / drawableWidth.toFloat()
        }
        matrix.setScale(scale, scale)
        imageMatrix = matrix
        return super.setFrame(l, t, r, b)
    }
}
