package com.anji.locationaccess.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.anji.location_sdk.data.LocationUpdateCallback
import com.anji.location_sdk.data.MapBuilder
import com.anji.locationaccess.R
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach


class LocationService : Service() {

    private val scopeJob = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationUpdate: LocationUpdateCallback
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        locationUpdate = MapBuilder(
            applicationContext,
            LocationServices.getFusedLocationProviderClient(applicationContext)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_LOGIN -> start()
            ACTION_LOGOUT -> stop()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun start() {
        val notification = NotificationCompat.Builder(this, "location")
            .setContentTitle("User Location")
            .setContentText("Tracking")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        locationUpdate.onLocationUpdate(2000L)
            .catch { e -> e.message }
            .onEach {
//                notificationManager.notify(1,)
            }
            .launchIn(scopeJob)
        startForeground(1, notification.build())
    }

    private fun stop() {
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        scopeJob.cancel()
    }

    companion object {
        const val ACTION_LOGIN = "START"
        const val ACTION_LOGOUT = "STOP"
    }
}