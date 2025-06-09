package com.example.flutter_background_sendport

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val CHANNEL = "com.example.flutter_background_sendport/bg"
    private val ISOLATE_CHANNEL = "com.example.flutter_background_sendport/isolated"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    // ✅ 修正点: メソッド名を "getCurrentWifiLocation" に変更
                    "getCurrentWifiLocation" -> {
                        val api = WifiLocationApiImpl(this)
                        val data = api.getCurrentWifiLocation()

                        // データを Dart isolate に送信（オプション）
                        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, ISOLATE_CHANNEL)
                            .invokeMethod("wifiLocation", data)

                        result.success("Data sent to Dart")
                    }

                    else -> result.notImplemented()
                }
            }
    }
}
