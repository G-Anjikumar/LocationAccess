package com.anji.locationaccess.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.anji.location_sdk.data.MapBuilder
import com.anji.location_sdk.viewmodel.MapViewModel
import com.anji.locationaccess.R
import com.anji.locationaccess.data.viewmodel.UserDataViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltWorker
class LocationWorker @AssistedInject constructor(
   @Assisted private val context: Context,
   @Assisted workerParams: WorkerParameters,
    private val mapBuilder: MapBuilder
) : CoroutineWorker(context, workerParams) {

    private var locationJob: Job? = null
    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = createNotification("Tracking location...")
        startForeground(1, notification)
    /*    locationJob = CoroutineScope(Dispatchers.IO).launch {
            mapViewModel.locationFlow.collectLatest { locationData ->
                locationData?.let { (isUserMoving, currentTime, location) ->
                    updateNotification(notificationManager, location)
                    saveLocationData(location, currentTime, isUserMoving)
                }
            }
        }
*/
        locationJob = CoroutineScope(Dispatchers.IO).launch {
            mapBuilder.onLocationUpdate(5000L).collect { (isUserMoving, currentTime, location) ->
                Log.d("LocationWorker", "Location: ${location.latitude}, ${location.longitude}")
                updateNotification(notificationManager, location)
            }
        }

        try {
            awaitCancellation() // Keep the worker alive until explicitly stopped
        } finally {
            locationJob?.cancel()
        }
        return Result.success()
    }
    private fun saveLocationData(location: Location, time: Long, isUserMoving: Boolean) {
        // Save to database or SharedPreferences
        Log.d(
            "LocationWorker",
            "Saving location: lat=${location.latitude}, lng=${location.longitude}, time=$time, moving=$isUserMoving"
        )
    }

    private fun createNotification(content: String): Notification {
        val stopIntent = WorkManager.getInstance(context).createCancelPendingIntent(id)
        return NotificationCompat.Builder(context, "location")
            .setContentTitle("User Location")
            .setContentText(content)
            .setSmallIcon(R.mipmap.ic_launcher)
            .addAction(0, "Stop", stopIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(
        notificationManager: NotificationManager,
        location: Location
    ) {
        Log.d("location_update", "updateNotification :: ${location.altitude}${location.longitude}")
        val updatedNotification = createNotification(
            "Location: ${location.latitude}, ${location.longitude}"
        )
        notificationManager.notify(1, updatedNotification)
    }

    private suspend fun startForeground(id: Int, notification: Notification) {
        val foregroundInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                id,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(id, notification)
        }
        setForeground(foregroundInfo)
    }
}
