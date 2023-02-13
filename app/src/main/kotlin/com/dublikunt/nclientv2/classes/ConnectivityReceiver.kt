package com.dublikunt.nclientv2.classes

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import com.dublikunt.nclientv2.utility.LogUtility.download

class ConnectivityReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        download("Is online? " + isOnline(context))
    }

    fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = cm.activeNetworkInfo
        //should check null because in airplane mode it will be null
        return netInfo != null && netInfo.isConnected
    }
}
