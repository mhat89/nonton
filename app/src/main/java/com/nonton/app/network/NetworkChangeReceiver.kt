package com.nonton.app.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log

class NetworkChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ConnectivityManager.CONNECTIVITY_ACTION) return

        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = network?.let(connectivityManager::getNetworkCapabilities)
        val hasInternet = capabilities?.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_INTERNET
        ) == true
        val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val isMetered = capabilities?.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_NOT_METERED
        ) == false

        Log.d(TAG, "Network changed - Internet: $hasInternet, WiFi: $isWifi, Metered: $isMetered")
    }

    companion object {
        private const val TAG = "NetworkChangeReceiver"
    }
}

class NetworkMonitor(private val context: Context) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
        as ConnectivityManager
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            this@NetworkMonitor.onNetworkAvailable(network)
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            this@NetworkMonitor.onNetworkLost()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            val hasInternet = networkCapabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET
            )
            val isWifi = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            val isMetered = networkCapabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_NOT_METERED
            ) == false
            this@NetworkMonitor.onNetworkCapabilitiesChanged(hasInternet, isWifi, isMetered)
        }
    }

    fun startMonitoring() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    fun stopMonitoring() {
        runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
    }

    open fun onNetworkAvailable(network: Network) = Unit
    open fun onNetworkLost() = Unit
    open fun onNetworkCapabilitiesChanged(hasInternet: Boolean, isWifi: Boolean, isMetered: Boolean) = Unit
}
