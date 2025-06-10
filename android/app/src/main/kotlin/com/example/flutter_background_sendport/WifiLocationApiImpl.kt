package com.example.flutter_background_sendport

import android.content.Context
import android.content.Intent
import android.util.Log
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class WifiLocationApiImpl(private val context: Context) : MethodChannel.MethodCallHandler {
    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "startForegroundService" -> {
                Log.d("WifiLocationApiImpl", "startForegroundService called")
                val intent = Intent(context, WifiLocationForegroundService::class.java)
                context.startForegroundService(intent)
                result.success(null)
            }
            "stopForegroundService" -> {
                Log.d("WifiLocationApiImpl", "stopForegroundService called")
                val intent = Intent(context, WifiLocationForegroundService::class.java)
                context.stopService(intent)
                result.success(null)
            }
            else -> {
                result.notImplemented()
            }
        }
    }
}
