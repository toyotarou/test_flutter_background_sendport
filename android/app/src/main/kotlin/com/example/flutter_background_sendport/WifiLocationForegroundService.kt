package com.example.flutter_background_sendport

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import com.example.flutter_background_sendport.util.WifiUtil
import com.example.flutter_background_sendport.util.LocationUtil

class WifiLocationForegroundService : Service() {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private var running = true

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        startLocationUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        running = false
        job.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun startForegroundService() {
        val channelId = "wifi_location_channel"
        val channelName = "WiFi Location Service"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan =
                NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(chan)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Wi-Fi位置取得中")
            .setContentText("Wi-Fiと位置情報を記録しています")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()

        startForeground(1, notification)
    }

    private fun startLocationUpdates() {
        scope.launch {
            while (running) {
                try {
                    val ssid = WifiUtil.getCurrentSsid(this@WifiLocationForegroundService)
                    val location = LocationUtil.getLastKnownLocation(this@WifiLocationForegroundService)
                    val lat = location?.first ?: 0.0
                    val lng = location?.second ?: 0.0

                    Log.d("WifiLocationService", "SSID=$ssid, Lat=$lat, Lng=$lng")

                    // ✅ Flutter 側に渡したい場合（例：SendPortなどを使う）
                    // BackgroundReceivePortSingleton.instance.port?.send(
                    //     "[backgroundHandler] {\"ssid\":\"$ssid\",\"lat\":$lat,\"lng\":$lng}"
                    // )

                    // ✅ または Kotlin 内で Room/Isarに保存する処理を追加

                } catch (e: Exception) {
                    Log.e("WifiLocationService", "Error: ${e.message}")
                }

                delay(60_000) // 1分ごとに実行
            }
        }
    }
}
