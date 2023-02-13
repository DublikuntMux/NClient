package com.dublikunt.nclientv2.components.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import com.dublikunt.nclientv2.R
import com.dublikunt.nclientv2.settings.DefaultDialogs
import com.dublikunt.nclientv2.settings.DefaultDialogs.CustomDialogResults
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.util.*
import kotlin.math.max
import kotlin.math.min

class PageSwitcher : MaterialCardView {
    private var prev: MaterialButton? = null
    private var next: MaterialButton? = null
    private var text: AppCompatEditText? = null
    private var changer: PageChanger? = null
    private var totalPage = 0
    var actualPage = 0

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
        setPages(0, 0)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context)
    }

    fun setChanger(changer: PageChanger?) {
        this.changer = changer
    }

    fun setPages(totalPage: Int, actualPage: Int) {
        var actualPage = actualPage
        actualPage = min(totalPage, max(actualPage, 1))
        val pageChanged = this.actualPage != actualPage
        if (this.totalPage == totalPage && !pageChanged) return
        this.totalPage = totalPage
        this.actualPage = actualPage
        if (pageChanged && changer != null) changer!!.pageChanged(this, actualPage)
        updateViews()
    }

    fun setTotalPage(totalPage: Int) {
        setPages(totalPage, actualPage)
    }

    private fun updateViews() {
        (context as AppCompatActivity).runOnUiThread {
            visibility = if (totalPage <= 1) GONE else VISIBLE
            prev!!.alpha = if (actualPage > 1) 1f else .5f
            prev!!.isEnabled = actualPage > 1
            next!!.alpha = if (actualPage < totalPage) 1f else .5f
            next!!.isEnabled = actualPage < totalPage
            text!!.setText(String.format(Locale.US, "%d / %d", actualPage, totalPage))
        }
    }

    private fun init(context: Context) {
        val master = LayoutInflater.from(context).inflate(R.layout.page_switcher, this, true)
            .findViewById<LinearLayout>(R.id.master_layout)
        prev = master.findViewById(R.id.prev)
        next = master.findViewById(R.id.next)
        text = master.findViewById(R.id.page_index)
        addViewListeners()
    }

    private fun addViewListeners() {
        next!!.setOnClickListener { if (changer != null) changer!!.onNextClicked(this) }
        prev!!.setOnClickListener { if (changer != null) changer!!.onPrevClicked(this) }
        text!!.setOnClickListener { loadDialog() }
    }

    fun lastPageReached(): Boolean {
        return actualPage == totalPage
    }

    private fun loadDialog() {
        DefaultDialogs.pageChangerDialog(
            DefaultDialogs.Builder(context)
                .setActual(actualPage)
                .setMin(1)
                .setMax(totalPage)
                .setTitle(R.string.change_page)
                .setDrawable(R.drawable.ic_find_in_page)
                .setDialogs(object : CustomDialogResults() {
                    override fun positive(actual: Int) {
                        actualPage = actual
                    }
                })
        )
    }

    interface PageChanger {
        fun pageChanged(switcher: PageSwitcher, page: Int)
        fun onPrevClicked(switcher: PageSwitcher)
        fun onNextClicked(switcher: PageSwitcher)
    }

    open class DefaultPageChanger : PageChanger {
        override fun pageChanged(switcher: PageSwitcher, page: Int) {}
        override fun onPrevClicked(switcher: PageSwitcher) {
            switcher.actualPage --
        }

        override fun onNextClicked(switcher: PageSwitcher) {
            switcher.actualPage ++
        }
    }
}
