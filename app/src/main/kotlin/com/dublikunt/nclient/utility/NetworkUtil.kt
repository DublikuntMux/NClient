package com.dublikunt.nclient.utility

import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.dublikunt.nclient.utility.LogUtility.download


object NetworkUtil {

    @Volatile
    var type = ConnectionType.WIFI
        set(x) {
            download("new Status: $x")
            field = x
        }

    private fun getConnectivity(
        cm: ConnectivityManager?,
        network: Network
    ): ConnectionType {
        val capabilities = cm!!.getNetworkCapabilities(network) ?: return ConnectionType.WIFI
        return if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) ConnectionType.CELLULAR else ConnectionType.WIFI
    }

    fun initConnectivity(context: Context) {
        var context = context
        context = context.applicationContext
        val cm = (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
        val builder = NetworkRequest.Builder()
        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        builder.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
        builder.addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
        builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        val callback: NetworkCallback = object : NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                type = getConnectivity(cm, network)
            }
        }
        try {
            cm.registerNetworkCallback(builder.build(), callback)
            val networks = cm.allNetworks
            if (networks.isNotEmpty()) type = getConnectivity(cm, networks[0])
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    enum class ConnectionType {
        NO_CONNECTION, WIFI, CELLULAR
    }
}
