package com.dublikunt.nclientv2.components.views

import android.content.Context
import android.content.DialogInterface
import android.view.View
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import com.dublikunt.nclientv2.R
import com.dublikunt.nclientv2.api.components.Gallery
import com.dublikunt.nclientv2.async.downloader.DownloadGalleryV2
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class RangeSelector(context: Context, private val gallery: Gallery) :
    MaterialAlertDialogBuilder(context) {
    private var s1: SeekBar? = null
    private var s2: SeekBar? = null

    init {
        val v = View.inflate(context, R.layout.range_selector, null)
        setView(v)
        val l1 = v.findViewById<LinearLayout>(R.id.layout1)
        val l2 = v.findViewById<LinearLayout>(R.id.layout2)
        applyLogic(l1, true)
        applyLogic(l2, false)
        setPositiveButton(R.string.ok) { _: DialogInterface?, which: Int ->
            if (s1!!.progress <= s2!!.progress) DownloadGalleryV2.downloadRange(
                context,
                gallery,
                s1!!.progress,
                s2!!.progress
            ) else Toast.makeText(context, R.string.invalid_range_selected, Toast.LENGTH_SHORT)
                .show()
        }.setNegativeButton(R.string.cancel, null)
        setCancelable(true)
    }

    private fun getPrevListener(s: SeekBar): View.OnClickListener {
        return View.OnClickListener { v: View? -> s.progress = s.progress - 1 }
    }

    private fun getNextListener(s: SeekBar): View.OnClickListener {
        return View.OnClickListener { v: View? -> s.progress = s.progress + 1 }
    }

    private fun getSeekBarListener(t: TextView): OnSeekBarChangeListener {
        return object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                t.text = context.getString(R.string.page_format, progress + 1, gallery.pageCount)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        }
    }

    private fun applyLogic(layout: LinearLayout, start: Boolean) {
        val prev = layout.findViewById<ImageButton>(R.id.prev)
        val next = layout.findViewById<ImageButton>(R.id.next)
        val pages = layout.findViewById<TextView>(R.id.pages)
        val seekBar = layout.findViewById<SeekBar>(R.id.seekBar)
        prev.setOnClickListener(getPrevListener(seekBar))
        next.setOnClickListener(getNextListener(seekBar))
        seekBar.max = gallery.pageCount - 1
        seekBar.setOnSeekBarChangeListener(getSeekBarListener(pages))
        seekBar.progress = 1
        seekBar.progress = if (start) 0 else gallery.pageCount
        if (start) s1 = seekBar else s2 = seekBar
    }
}
