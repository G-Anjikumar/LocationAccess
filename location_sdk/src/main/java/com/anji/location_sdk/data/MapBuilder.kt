package com.anji.location_sdk.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.provider.Settings
import android.util.Log
import com.anji.location_sdk.util.hasLocationPermission
//import com.anji.location_sdk.util.hasLocationPermission
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MapBuilder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: FusedLocationProviderClient
) : LocationUpdateCallback {

    private var lastLocation: Location? = null
    private var lastUpdateTime: Long = 0L
    private var isUserMoving = false

    @SuppressLint("MissingPermission")
    override fun onLocationUpdate(interval: Long): Flow<Triple<Boolean, Long, Location>> {
        return callbackFlow {
            if (!context.hasLocationPermission()) {
                throw LocationUpdateCallback.locationException("Need Location Permission")
            }

            val locationManager =
                context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled =
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!isGpsEnabled && !isNetworkEnabled) {
                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }

            val request = LocationRequest.create()
                .setInterval(interval)
                .setFastestInterval(1000L)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setMaxWaitTime(interval)

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    super.onLocationResult(result)
                    result.locations.lastOrNull()?.let { location ->
                        Log.d("location_update", "In_Mapper:: ${location.longitude}")
                        val currentTime = System.currentTimeMillis()
                        Log.d("location_update", "In_Mapper:: $currentTime")
                        if (lastLocation != null) {
                            val timeDifference = (currentTime - lastUpdateTime) / 1000.0 // seconds
                            val distance = lastLocation!!.distanceTo(location) // meters
                            val speed = if (timeDifference > 0) distance / timeDifference else 0.0 // m/s
                            // Adjust the speed threshold based on your use case
                            isUserMoving = speed > 0.3 // 0.2 m/s (about 1.8 km/h, a slow walk)
                            lastUpdateTime = currentTime
                        }
                        lastLocation = location
                        client.lastLocation.addOnSuccessListener { loc ->
                            loc?.let {
                                launch { trySend(Triple(isUserMoving, currentTime, it)) }
                            }
                        }
                    }
                }
            }
            client.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())

            awaitClose {
                client.removeLocationUpdates(locationCallback)
            }
        }
    }
}