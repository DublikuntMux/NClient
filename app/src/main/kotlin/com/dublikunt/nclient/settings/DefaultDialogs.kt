package com.dublikunt.nclient.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.text.Editable
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.TextWatcher
import android.view.View
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.dublikunt.nclient.R
import com.dublikunt.nclient.settings.Global.useRtl
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import java.util.*

object DefaultDialogs {
    fun pageChangerDialog(builder: Builder) {
        val build = MaterialAlertDialogBuilder(builder.context)
        if (builder.title != 0) build.setTitle(builder.context.getString(builder.title))
        if (builder.drawable != 0) build.setIcon(builder.drawable)
        val v = View.inflate(builder.context, R.layout.page_changer, null)
        build.setView(v)
        val seekBar = v.findViewById<SeekBar>(R.id.seekBar)
        if (useRtl()) seekBar.rotationY = 180f
        val totalPage = v.findViewById<TextView>(R.id.page)
        val actualPage = v.findViewById<TextInputEditText>(R.id.edit_page)
        v.findViewById<View>(R.id.prev).setOnClickListener { v12: View? ->
            seekBar.progress = seekBar.progress - 1
            actualPage.setText(String.format(Locale.US, "%d", seekBar.progress + builder.min))
        }
        v.findViewById<View>(R.id.next).setOnClickListener { v1: View? ->
            seekBar.progress = seekBar.progress + 1
            actualPage.setText(String.format(Locale.US, "%d", seekBar.progress + builder.min))
        }
        seekBar.max = builder.max - builder.min
        seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) actualPage.setText(
                    String.format(
                        Locale.US,
                        "%d",
                        progress + builder.min
                    )
                )
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        actualPage.setText(String.format(Locale.US, "%d", builder.actual))
        seekBar.progress = builder.actual - builder.min
        totalPage.text = String.format(Locale.US, "%d", builder.max)
        val filterArray = arrayOfNulls<InputFilter>(1)
        filterArray[0] = LengthFilter(builder.max.toString().length)
        actualPage.filters = filterArray
        actualPage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                val x: Int = try {
                    s.toString().toInt()
                } catch (e: NumberFormatException) {
                    -1
                }
                if (x < builder.min) seekBar.progress = 0 else seekBar.progress = x - builder.min
            }
        })
        if (builder.dialogs != null) build
            .setPositiveButton(builder.context.getString(builder.yesbtn)) { dialog: DialogInterface?, id: Int ->
                builder.dialogs!!.positive(
                    seekBar.progress + builder.min
                )
            }
            .setNegativeButton(builder.context.getString(builder.nobtn)) { dialog: DialogInterface?, which: Int -> builder.dialogs!!.negative() }
        if (builder.maybebtn != 0) build.setNeutralButton(builder.context.getString(builder.maybebtn)) { dialog: DialogInterface?, which: Int -> builder.dialogs!!.neutral() }
        build.setCancelable(true)
        build.show()
    }

    interface DialogResults {
        fun positive(actual: Int)
        fun negative()
        fun neutral()
    }

    open class CustomDialogResults : DialogResults {
        override fun positive(actual: Int) {}
        override fun negative() {}
        override fun neutral() {}
    }

    @SuppressLint("ResourceType")
    class Builder(val context: Context) {
        var dialogs: DialogResults?

        @StringRes
        var title: Int

        @StringRes
        var yesbtn: Int

        @StringRes
        var nobtn: Int

        @StringRes
        var maybebtn: Int

        @DrawableRes
        var drawable = 0
        var max: Int
        var actual: Int
        var min: Int

        init {
            title = drawable
            yesbtn = R.string.ok
            nobtn = R.string.cancel
            maybebtn = 0
            actual = 1
            max = actual
            min = 0
            dialogs = null
        }

        fun setMin(min: Int): Builder {
            this.min = min
            return this
        }

        fun setTitle(title: Int): Builder {
            this.title = title
            return this
        }

        fun setYesbtn(yesbtn: Int): Builder {
            this.yesbtn = yesbtn
            return this
        }

        fun setNobtn(nobtn: Int): Builder {
            this.nobtn = nobtn
            return this
        }

        fun setDrawable(drawable: Int): Builder {
            this.drawable = drawable
            return this
        }

        fun setMax(max: Int): Builder {
            this.max = max
            return this
        }

        fun setMaybebtn(maybebtn: Int): Builder {
            this.maybebtn = maybebtn
            return this
        }

        fun setActual(actual: Int): Builder {
            this.actual = actual
            return this
        }

        fun setDialogs(dialogs: DialogResults?): Builder {
            this.dialogs = dialogs
            return this
        }
    }
}
