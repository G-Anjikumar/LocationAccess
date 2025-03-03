package com.anji.locationaccess.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.anji.locationaccess.R
import com.anji.locationaccess.data.local.repo.user.UserRepository
import com.anji.locationaccess.data.local.repo.user.UserRepositoryImplementation
import com.anji.locationaccess.data.model.UserDetails
import com.anji.locationaccess.di.RoomDatabaseInstance
import com.anji.locationaccess.util.AppConstants
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import java.util.Locale

//@HiltWorker
class LocationWorker(
    private val context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    private val client = LocationServices.getFusedLocationProviderClient(context)
    private var lastLocation: Location? = null
    private var isUserMoving = false
    private var lastActiveTimeLocal: Long = 0L
    private var lastIdleTimeLocal: Long = 0L
    private var totalActiveTimeLocal: Long = 0L
    private var totalIdleTimeLocal: Long = 0L
    private var locationJob: Job? = null
    private val gson = Gson()
    private var place = ""
    private var road = ""
    private var latlng = ""
    private lateinit var userDetails: UserDetails
    private var elementList: ArrayList<String> = ArrayList()
    private val userRepository: UserRepository =
        UserRepositoryImplementation(RoomDatabaseInstance.provideRoomDatabase(context).userDao())

    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result {
        userDetails = UserDetails()
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        Log.d("locationWorker", "client :: $client")
        Log.d("locationWorker", "userRepository :: $userRepository")
        val notification = createNotification("Tracking location...")
        val nameLocal = inputData.getString(AppConstants.name)
        val mobileNumber = inputData.getString(AppConstants.mobileNumber)
        val userId = inputData.getLong(AppConstants.userId, 0L)
        lastActiveTimeLocal=inputData.getLong(AppConstants.lastActiveTimeLocal,0L)
        lastIdleTimeLocal=inputData.getLong(AppConstants.lastIdleTimeLocal,0L)
        totalActiveTimeLocal=inputData.getLong(AppConstants.totalActiveTimeLocalr,0L)
        totalIdleTimeLocal=inputData.getLong(AppConstants.totalIdleTimeLocal,0L)
        startForeground(1, notification)
        var lastLocation: Location? = null
        var lastUpdateTime: Long = 0L

        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 10000L
            fastestInterval = 5000L
        }

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.lastOrNull()?.let { location ->
                    val currentTime = System.currentTimeMillis()
                    if (lastLocation != null) {
                        val timeDifference = (currentTime - lastUpdateTime) / 1000.0
                        val distance = lastLocation!!.distanceTo(location)
                        val speed = if (timeDifference > 0) distance / timeDifference else 0.0
                        isUserMoving = speed > 0.3
                        lastUpdateTime = currentTime
                    }
                    if (isUserMoving) {
                        if (lastActiveTimeLocal == 0L) {
                            lastActiveTimeLocal = currentTime
                        }
                        totalActiveTimeLocal += currentTime - lastActiveTimeLocal
                        lastActiveTimeLocal = currentTime
                    } else {
                        if (lastIdleTimeLocal == 0L) {
                            lastIdleTimeLocal = currentTime
                        }
                        totalIdleTimeLocal += currentTime - lastIdleTimeLocal
                        lastIdleTimeLocal = currentTime
                    }
                    updateNotification(notificationManager, location)
                    lastLocation = location
                    updateLocationValues(location, userId, nameLocal!!, mobileNumber!!)
                }
            }
        }

        client.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        try {
            awaitCancellation()
        } finally {
            client.removeLocationUpdates(locationCallback)
        }

        return Result.success()
    }

    private fun createNotification(content: String): Notification {
        val stopIntent = WorkManager.getInstance(context).createCancelPendingIntent(id)
        return NotificationCompat.Builder(context, "location")
            .setContentTitle("User Location")
            .setContentText(content)
            .setSmallIcon(R.drawable.notificaton_location)
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

    fun updateLocationValues(
        location: Location,
        userId: Long,
        nameLocal: String,
        mobileNumberLocal: String
    ) {
        val geocoder = Geocoder(context, Locale.getDefault())
        val addressWithLatLong: MutableList<Address>? =
            geocoder.getFromLocation(location.latitude, location.longitude, 1)
        elementList.clear()
        if (addressWithLatLong != null) {
            place =
                addressWithLatLong[0].locality + ", " + addressWithLatLong[0].adminArea + ", " + addressWithLatLong[0].countryName
            road = addressWithLatLong[0].getAddressLine(0)
            elementList.add(place)
            elementList.add(road)
            latlng = "Lat Lng : ${location.latitude}, ${location.longitude}"
            elementList.add(latlng)
        }
        val addressString: String = gson.toJson(elementList)
        if (userDetails == null) {
            userDetails = UserDetails(
                id = userId,
                name = nameLocal,
                mobileNumber = mobileNumberLocal,
                timeStamp = System.currentTimeMillis(),
                lastActiveTime = lastActiveTimeLocal,
                lastIdleTime = lastIdleTimeLocal,
                idleTime = totalIdleTimeLocal,
                activeTime = totalActiveTimeLocal,
                address = addressString,
                lantitude = location.latitude.toString(),
                longitude = location.longitude.toString()
            )
        } else {
            userDetails.apply {
                id = userId
                name = nameLocal
                mobileNumber = mobileNumberLocal
                timeStamp = System.currentTimeMillis()
                lastActiveTime = lastActiveTimeLocal
                lastIdleTime = lastIdleTimeLocal
                idleTime = totalIdleTimeLocal
                activeTime = totalActiveTimeLocal
                address = addressString
                lantitude = location.latitude.toString()
                longitude = location.longitude.toString()
                timeStamp = System.currentTimeMillis()
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            userRepository.updateUserDetails(userDetails)
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(1, createNotification("Location Tracking"))
    }
}
