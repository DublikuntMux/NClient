package com.dublikunt.nclient

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import com.dublikunt.nclient.settings.Global
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import java.util.Arrays

class PINActivity : GeneralActivity() {
    private lateinit var numbers: List<MaterialButton>
    private lateinit var texts: MutableList<MaterialTextView>
    private lateinit var cancelButton: MaterialButton
    private lateinit var text: MaterialTextView
    private var pin = ""
    private var confirmPin: String? = null
    private var setMode = false
    private var preferences: SharedPreferences? = null
    private var pinLenght = 4
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Global.initActivity(this);
        setContentView(R.layout.activity_pin)
        preferences = getSharedPreferences("Settings", 0)
        setMode = intent.getBooleanExtra("$packageName.SET", false)
        if (!setMode && !hasPin()) {
            finish()
            return
        }
        val logo = findViewById<ImageView>(R.id.imageView)
        logo.setImageResource(if (Global.getTheme() == Global.ThemeScheme.LIGHT) R.drawable.ic_logo_dark else R.drawable.ic_logo)
        val linear = findViewById<LinearLayout>(R.id.linearLayout)
        text = findViewById(R.id.textView)
        cancelButton = findViewById(R.id.cancelButton)
        texts = ArrayList(pinLenght)
        for (i in 0 until pinLenght) texts.add(linear.getChildAt(i) as MaterialTextView)
        numbers = listOf(
            findViewById(R.id.btn0), findViewById(R.id.btn1), findViewById(R.id.btn2),
            findViewById(R.id.btn3), findViewById(R.id.btn4), findViewById(R.id.btn5),
            findViewById(R.id.btn6), findViewById(R.id.btn7), findViewById(R.id.btn8),
            findViewById(R.id.btn9)
        )
        for (i in numbers.indices) {
            numbers[i].setOnClickListener {
                pin += i
                applyPinMask()
                if (pin.length == 4) checkPin()
            }
        }
        cancelButton.setOnClickListener(View.OnClickListener {
            if (pin.isEmpty()) return@OnClickListener
            pin = pin.substring(0, pin.length - 1)
            applyPinMask()
        })
        cancelButton.setOnLongClickListener {
            pin = ""
            applyPinMask()
            true
        }
        val utility = findViewById<MaterialButton>(R.id.utility)
        utility.setOnClickListener { v: View? ->
            if (setMode && isConfirming) {
                checkPin()
            } else {
                finish()
            }
        }
    }

    private val isConfirming: Boolean
        private get() = confirmPin != null

    private fun hasPin(): Boolean {
        return preferences!!.getBoolean("has_pin", false)
    }

    private fun setPin(pin: String?) {
        val editor = preferences!!.edit()
        editor.putBoolean("has_pin", true)
        editor.putString("pin", pin)
        editor.apply()
    }

    override fun finish() {
        val i = Intent(this, if (setMode) SettingsActivity::class.java else MainActivity::class.java)
        if (setMode || !hasPin() || pin == truePin) startActivity(i)
        super.finish()
    }

    private val truePin: String?
        private get() = preferences!!.getString("pin", null)

    private fun checkPin() {
        if (setMode) {
            if (!isConfirming) {
                confirmPin = pin
                pin = ""
                text.setText(R.string.confirm_pin)
            } else if (confirmPin == pin) {
                setPin(confirmPin)
                finish()
            } else {
                confirmPin = null
                text.setText(R.string.insert_pin)
                pin = ""
            }
        } else if (pin == truePin) {
            finish()
        } else {
            text.setText(R.string.wrong_pin)
            pin = ""
        }
        applyPinMask()
    }

    private fun applyPinMask() {
        val len = pin.length.coerceAtMost(pinLenght)
        var i = 0
        while (i < len) {
            texts[i].setText(R.string.full_circle)
            i++
        }
        while (i < pinLenght) {
            texts[i].setText(R.string.empty_circle)
            i++
        }
    }
}
