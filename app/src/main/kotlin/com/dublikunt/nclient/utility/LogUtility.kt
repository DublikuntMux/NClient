package com.dublikunt.nclient.utility

import android.util.Log

object LogUtility {
    private const val LOGTAG = "NCLIENTLOG"

    @JvmStatic
    fun download(vararg message: Any) {
        if (message.size == 1) Log.d(LOGTAG, "" + message[0]) else Log.d(
            LOGTAG, message.contentToString()
        )
    }

    fun download(message: String?, throwable: Throwable?) {
        var message = message
        if (message == null) message = ""
        Log.d(LOGTAG, message, throwable)
    }

    fun info(vararg message: Any) {
        if (message.size == 1) Log.i(LOGTAG, "" + message[0]) else Log.i(
            LOGTAG, message.contentToString()
        )
    }

    fun info(message: String?, throwable: Throwable?) {
        var message = message
        if (message == null) message = ""
        Log.i(LOGTAG, message, throwable)
    }

    fun error(vararg message: Any) {
        if (message.size == 1) Log.e(LOGTAG, "" + message[0]) else Log.e(
            LOGTAG, message.contentToString()
        )
    }

    @JvmStatic
    fun error(message: Any?, throwable: Throwable?) {
        var message = message
        if (message == null) message = ""
        Log.e(LOGTAG, message.toString(), throwable)
    }
}
