package com.vegerot.larklish

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONObject

/** Debug-only network diagnostics. A VPN transport does not prove corporate reachability. */
internal class NetworkState(context: Context) : AutoCloseable {
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)
    private val wifi = ConcurrentHashMap<Network, WifiInfo>()
    // getNetworkCapabilities() redacts the SSID even with location access. A callback with this
    // flag preserves it when location access is granted and the phone's Location setting is on.
    private val callback =
        object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                (caps.transportInfo as? WifiInfo)?.let { wifi[network] = it }
            }

            override fun onLost(network: Network) {
                wifi.remove(network)
            }
        }

    init {
        // Observe the physical Wi-Fi network even when a VPN is the app's default network.
        connectivity.registerNetworkCallback(
            NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build(),
            callback,
        )
    }

    fun snapshot(): JSONObject {
        val active = connectivity.getNetworkCapabilities(connectivity.activeNetwork)
        val info = wifi.values.firstOrNull() // The test Pixel uses one Wi-Fi connection.
        val ssid =
            info?.ssid?.takeUnless { it == WifiManager.UNKNOWN_SSID }?.removeSurrounding("\"")
        return JSONObject()
            // Whether a VPN applies to this app's default network, not merely an installed VPN app.
            .put("vpn", active?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true)
            .put(
                "wifiConnected",
                info != null || active?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true,
            )
            // null while disconnected, redacted, or waiting for the first Wi-Fi callback.
            // wifiConnected distinguishes an unavailable SSID from no Wi-Fi connection.
            .put("wifiSsid", ssid ?: JSONObject.NULL)
    }

    override fun close() {
        connectivity.unregisterNetworkCallback(callback)
    }
}
