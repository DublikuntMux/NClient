package com.dublikunt.nclient.components.views

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.cardview.widget.CardView
import com.dublikunt.nclient.R
import com.dublikunt.nclient.settings.DefaultDialogs
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.util.*

class PageSwitcher : CardView {
    private lateinit var master: LinearLayout
    private lateinit var prev: MaterialButton
    private lateinit var next: MaterialButton
    private lateinit var text: TextInputEditText
    private lateinit var changer: PageChanger
    private var totalPage = 0
    private var actualPage = 0

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context)
        setPages(0, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context)
    }

    fun setChanger(changer: PageChanger) {
        this.changer = changer
    }

    fun setPages(totalPage: Int, actualPage: Int) {
        var actualPage = actualPage
        actualPage = totalPage.coerceAtMost(actualPage.coerceAtLeast(1))
        val pageChanged = this.actualPage != actualPage
        if (this.totalPage == totalPage && !pageChanged) return
        this.totalPage = totalPage
        this.actualPage = actualPage
        if (pageChanged) changer.pageChanged(this, actualPage)
        updateViews()
    }

    fun setTotalPage(totalPage: Int) {
        setPages(totalPage, actualPage)
    }

    private fun updateViews() {
        (context as Activity).runOnUiThread {
            visibility = if (totalPage <= 1) GONE else VISIBLE
            prev.alpha = if (actualPage > 1) 1f else .5f
            prev.isEnabled = actualPage > 1
            next.alpha = if (actualPage < totalPage) 1f else .5f
            next.isEnabled = actualPage < totalPage
            text.setText(
                String.format(
                    Locale.US,
                    "%d / %d",
                    actualPage,
                    totalPage
                )
            )
        }
    }

    private fun init(context: Context) {
        master = LayoutInflater.from(context).inflate(R.layout.page_switcher, this, true)
            .findViewById<View>(R.id.master_layout) as LinearLayout
        prev = master.findViewById(R.id.prev)
        next = master.findViewById(R.id.next)
        text = master.findViewById(R.id.page_index)
        addViewListeners()
    }

    private fun addViewListeners() {
        next.setOnClickListener {
            changer.onNextClicked(
                this
            )
        }
        prev.setOnClickListener {
            changer.onPrevClicked(
                this
            )
        }
        text.setOnClickListener { loadDialog() }
    }

    fun getActualPage(): Int {
        return actualPage
    }

    fun setActualPage(actualPage: Int) {
        setPages(totalPage, actualPage)
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
                .setDialogs(object : DefaultDialogs.CustomDialogResults() {
                    override fun positive(actual: Int) {
                        setActualPage(actual)
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
            switcher.setActualPage(switcher.getActualPage() - 1)
        }

        override fun onNextClicked(switcher: PageSwitcher) {
            switcher.setActualPage(switcher.getActualPage() + 1)
        }
    }
}
