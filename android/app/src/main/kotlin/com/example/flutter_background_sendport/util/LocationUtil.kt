package com.example.flutter_background_sendport.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager

object LocationUtil {
    @SuppressLint("MissingPermission")
    fun getLastKnownLocation(context: Context): Pair<Double, Double>? {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = locationManager.getProviders(true)

        for (provider in providers.reversed()) {
            val location: Location? = locationManager.getLastKnownLocation(provider)
            if (location != null) {
                return Pair(location.latitude, location.longitude)
            }
        }
        return null
    }
}
