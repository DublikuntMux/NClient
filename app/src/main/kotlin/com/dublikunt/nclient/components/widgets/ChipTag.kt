package com.dublikunt.nclient.components.widgets

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.dublikunt.nclient.R
import com.dublikunt.nclient.api.comments.Tag
import com.dublikunt.nclient.enums.TagStatus
import com.dublikunt.nclient.settings.Global
import com.google.android.material.chip.Chip

class ChipTag : Chip {
    private lateinit var tag: Tag
    private var canBeAvoided = true

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    fun init(t: Tag, close: Boolean, canBeAvoided: Boolean) {
        setTag(t)
        isCloseIconVisible = close
        setCanBeAvoided(canBeAvoided)
    }

    private fun setCanBeAvoided(canBeAvoided: Boolean) {
        this.canBeAvoided = canBeAvoided
    }

    override fun getTag(): Tag {
        return tag!!
    }

    private fun setTag(tag: Tag) {
        this.tag = tag
        text = tag.name
        loadStatusIcon()
    }

    fun changeStatus(status: TagStatus) {
        tag!!.status = status
        loadStatusIcon()
    }

    fun updateStatus() {
        when (tag!!.status) {
            TagStatus.DEFAULT -> changeStatus(TagStatus.ACCEPTED)
            TagStatus.ACCEPTED -> changeStatus(if (canBeAvoided) TagStatus.AVOIDED else TagStatus.DEFAULT)
            TagStatus.AVOIDED -> changeStatus(TagStatus.DEFAULT)
        }
    }

    private fun loadStatusIcon() {
        val drawable = ContextCompat.getDrawable(
            context,
            if (tag!!.status == TagStatus.ACCEPTED) R.drawable.ic_check else if (tag!!.status == TagStatus.AVOIDED) R.drawable.ic_close else R.drawable.ic_void
        )
        if (drawable == null) {
            setChipIconResource(if (tag!!.status == TagStatus.ACCEPTED) R.drawable.ic_check else if (tag!!.status == TagStatus.AVOIDED) R.drawable.ic_close else R.drawable.ic_void)
            return
        }
        chipIcon = drawable
        Global.setTint(chipIcon)
    }
}
