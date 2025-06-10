package com.example.flutter_background_sendport.util

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build

object WifiUtil {
    fun getCurrentSsid(context: Context): String? {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val info = wifiManager.connectionInfo

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11以上はSSID取得に制限あり（取得できない場合はnull）
            if (info.ssid == WifiManager.UNKNOWN_SSID) null else info.ssid.replace("\"", "")
        } else {
            info.ssid?.replace("\"", "")
        }
    }
}
