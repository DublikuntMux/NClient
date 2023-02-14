package com.dublikunt.nclient.components.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.widget.TextView
import androidx.appcompat.widget.SearchView

class CustomSearchView : SearchView {
    constructor(context: Context?) : super(context!!)
    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!, attrs
    )

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context!!, attrs, defStyleAttr
    )

    override fun setOnQueryTextListener(listener: OnQueryTextListener) {
        super.setOnQueryTextListener(listener)
        val mSearchSrcTextView =
            findViewById<SearchAutoComplete>(androidx.appcompat.R.id.search_src_text)
        mSearchSrcTextView.setOnEditorActionListener { _: TextView?, _: Int, _: KeyEvent? ->
            listener.onQueryTextSubmit(query.toString())
            true
        }
    }
}
