package com.dublikunt.nclientv2

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.dublikunt.nclientv2.MainActivity
import com.dublikunt.nclientv2.components.activities.GeneralActivity
import com.dublikunt.nclientv2.settings.Global
import com.google.android.material.button.MaterialButton

class PINActivity : GeneralActivity() {
    private lateinit var texts: MutableList<TextView>
    private lateinit var text: TextView
    private var pin = ""
    private var confirmPin: String? = null
    private var setMode = false
    private var preferences: SharedPreferences? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin)
        preferences = getSharedPreferences("Settings", 0)
        setMode = intent.getBooleanExtra("$packageName.SET", false)
        if (!setMode && !hasPin()) {
            finish()
            return
        }
        val logo = findViewById<ImageView>(R.id.imageView)
        logo.setImageResource(if (Global.theme == Global.ThemeScheme.LIGHT) R.drawable.ic_logo_dark else R.drawable.ic_logo)
        val linear = findViewById<LinearLayout>(R.id.linearLayout)
        text = findViewById(R.id.textView)
        val cancelButton = findViewById<MaterialButton>(R.id.cancelButton)
        texts = ArrayList(PIN_LENGHT)
        for (i in 0 until PIN_LENGHT) texts.add(linear.getChildAt(i) as TextView)
        val numbers = listOf(
            findViewById(R.id.btn0), findViewById(R.id.btn1), findViewById(R.id.btn2),
            findViewById(R.id.btn3), findViewById(R.id.btn4), findViewById(R.id.btn5),
            findViewById(R.id.btn6), findViewById(R.id.btn7), findViewById(R.id.btn8),
            findViewById<MaterialButton>(R.id.btn9)
        )
        for (i in numbers.indices) {
            numbers[i].setOnClickListener { v: View? ->
                pin += i
                applyPinMask()
                if (pin.length == 4) checkPin()
            }
        }
        cancelButton.setOnClickListener { v: View? ->
            if (pin.isEmpty()) return@setOnClickListener
            pin = pin.substring(0, pin.length - 1)
            applyPinMask()
        }
        cancelButton.setOnLongClickListener { v: View? ->
            pin = ""
            applyPinMask()
            true
        }
        val utility = findViewById<MaterialButton>(R.id.utility)
        utility.setOnClickListener { v: View? ->
            if (setMode && isConfirming) {
                checkPin() //will go in wrong branch
            } else {
                finish()
            }
        }
    }

    private val isConfirming: Boolean
        get() = confirmPin != null

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
        val i =
            Intent(this, if (setMode) SettingsActivity::class.java else MainActivity::class.java)
        if (setMode || !hasPin() || pin == truePin) startActivity(i)
        super.finish()
    }

    private val truePin: String?
        get() = preferences!!.getString("pin", null)

    private fun checkPin() {
        if (setMode) { //if password should be set
            if (!isConfirming) { //now password must be confirmed
                confirmPin = pin
                pin = ""
                text.setText(R.string.confirm_pin)
            } else if (confirmPin == pin) { //password confirmed
                setPin(confirmPin)
                finish()
            } else { //wrong confirmed password
                confirmPin = null
                text.setText(R.string.insert_pin)
                pin = ""
            }
        } else if (pin == truePin) { //right password
            finish()
        } else { //wrong password
            text.setText(R.string.wrong_pin)
            pin = ""
        }
        applyPinMask()
    }

    private fun applyPinMask() {
        val len = pin.length.coerceAtMost(PIN_LENGHT)
        var i = 0
        while (i < len) {
            texts[i].setText(R.string.full_circle)
            i++
        }
        while (i < PIN_LENGHT) {
            texts[i].setText(R.string.empty_circle)
            i++
        }
    }

    companion object {
        private const val PIN_LENGHT = 4
    }
}
