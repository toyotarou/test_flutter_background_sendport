package com.example.flutter_background_sendport

import android.content.Intent
import android.util.Log
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import com.example.flutter_background_sendport.pigeon.WifiLocationServiceApi
import org.json.JSONObject

import com.example.flutter_background_sendport.util.WifiUtil
import com.example.flutter_background_sendport.util.LocationUtil

class MainActivity : FlutterActivity(), MethodChannel.MethodCallHandler {
    private val CHANNEL = "com.example.flutter_background_sendport/bg"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        // 🔧 MethodChannel 経由の Wi-Fi 情報取得
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
            .setMethodCallHandler(this)

        // 🔽 Pigeon 経由の ForegroundService 開始／停止制御
        val api: WifiLocationServiceApi = WifiLocationServiceApiImpl(this)
        WifiLocationServiceApi.setUp(flutterEngine.dartExecutor.binaryMessenger, api)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "startForegroundService" -> {
                Log.d("MainActivity", "startForegroundService called")
                val intent = Intent(this, WifiLocationForegroundService::class.java)
                startForegroundService(intent)
                result.success("started")
            }

            "getCurrentWifiLocation" -> {
                Log.d("MainActivity", "getCurrentWifiLocation called")

                val ssid = WifiUtil.getCurrentSsid(this)
                val latLng = LocationUtil.getLastKnownLocation(this)

                val data = mapOf(
                    "ssid" to (ssid ?: "unknown"),
                    "lat" to (latLng?.first?.toString() ?: "0.0"),
                    "lng" to (latLng?.second?.toString() ?: "0.0")
                )

                result.success(JSONObject(data).toString())
            }

            else -> result.notImplemented()
        }
    }
}
