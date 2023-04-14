package com.dublikunt.nclient

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import com.dublikunt.nclient.components.activities.GeneralActivity

class CopyToClipboardActivity : GeneralActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = intent.data
        if (uri != null) {
            copyTextToClipboard(this, uri.toString())
            Toast.makeText(this, R.string.link_copied_to_clipboard, Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    companion object {
        fun copyTextToClipboard(context: Context, text: String) {
            val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("text", text)
            clipboard.setPrimaryClip(clip)
        }
    }
}
