package com.example.flutter_background_sendport

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.net.wifi.WifiManager
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import org.json.JSONObject

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority

class WifiLocationApiImpl(private val context: Context) : MethodChannel.MethodCallHandler {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        if (call.method == "getCurrentWifiLocation") {
            getWifiAndLocation(result)
        } else {
            result.notImplemented()
        }
    }

    @SuppressLint("MissingPermission", "HardwareIds")
    private fun getWifiAndLocation(result: MethodChannel.Result) {
        // パーミッション確認
        val hasLocationPermission = ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasLocationPermission) {
            result.error("PERMISSION_DENIED", "Location permission not granted", null)
            return
        }

        // Wi-Fi SSID の取得
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        val ssid = wifiInfo.ssid.removePrefix("\"").removeSuffix("\"")

        // 現在地の取得
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val lat = location.latitude
                val lng = location.longitude

                val json = JSONObject()
                json.put("ssid", ssid)
                json.put("lat", lat)
                json.put("lng", lng)

                result.success(json.toString())
            } else {
                // fallback: request new location if lastLocation is null
                requestNewLocation(result, ssid)
            }
        }.addOnFailureListener {
            result.error("LOCATION_ERROR", "Failed to get location", null)
        }
    }

    private fun requestNewLocation(result: MethodChannel.Result, ssid: String) {
        val locationRequest =
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L).build()

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    fusedLocationClient.removeLocationUpdates(this)
                    val location = locationResult.lastLocation
                    if (location != null) {
                        val json = JSONObject()
                        json.put("ssid", ssid)
                        json.put("lat", location.latitude)
                        json.put("lng", location.longitude)
                        result.success(json.toString())
                    } else {
                        result.error("LOCATION_ERROR", "Location is null", null)
                    }
                }
            },
            Looper.getMainLooper()
        )
    }
}
