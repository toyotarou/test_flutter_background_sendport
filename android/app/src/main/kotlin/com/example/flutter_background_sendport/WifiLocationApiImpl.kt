package com.example.flutter_background_sendport

import android.content.Context
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import org.json.JSONObject

class WifiLocationApiImpl(private val context: Context) : MethodCallHandler {

    override fun onMethodCall(call: MethodCall, result: Result) {
        if (call.method == "getCurrentWifiLocation") {
            val ssid = "aterm-f1db10-a"
            val lat = 35.718389016152436
            val lng = 139.5869888374933

            // JSON 形式で返す
            val json = JSONObject()
            json.put("ssid", ssid)
            json.put("lat", lat)
            json.put("lng", lng)

            result.success(json.toString()) // ← 文字列として返す
        } else {
            result.notImplemented()
        }
    }
}
