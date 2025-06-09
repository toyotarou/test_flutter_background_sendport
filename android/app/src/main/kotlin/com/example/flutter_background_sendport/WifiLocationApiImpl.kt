package com.example.flutter_background_sendport

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.util.Log
import androidx.core.content.ContextCompat

class WifiLocationApiImpl(private val context: Context) {

    fun getCurrentWifiLocation(): Map<String, String> {
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        val ssid = wifiInfo.ssid?.replace("\"", "") ?: "Unknown SSID"

        var lat = "0.0"
        var lng = "0.0"

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            val locationManager =
                context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val location: Location? =
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            if (location != null) {
                lat = location.latitude.toString()
                lng = location.longitude.toString()
            }
        } else {
            Log.w("WifiLocationApi", "⚠️ ACCESS_FINE_LOCATION not granted")
        }

        val result = mapOf(
            "ssid" to ssid,
            "lat" to lat,
            "lng" to lng
        )

        Log.d("WifiLocationApi", "📡 実測Wi-Fi位置情報: $result")

        return result
    }
}
