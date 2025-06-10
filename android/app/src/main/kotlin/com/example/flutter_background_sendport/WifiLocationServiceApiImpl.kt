package com.example.flutter_background_sendport

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.flutter_background_sendport.pigeon.WifiLocationServiceApi

class WifiLocationServiceApiImpl(private val context: Context) : WifiLocationServiceApi {

    override fun startService() {
        Log.d("WifiLocationServiceApi", "startService called")
        val intent = Intent(context, WifiLocationForegroundService::class.java)
        context.startForegroundService(intent)
    }

    override fun stopService() {
        Log.d("WifiLocationServiceApi", "stopService called")
        val intent = Intent(context, WifiLocationForegroundService::class.java)
        context.stopService(intent)
    }
}
