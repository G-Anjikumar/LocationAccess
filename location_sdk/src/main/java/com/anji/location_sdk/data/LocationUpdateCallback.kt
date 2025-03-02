package com.anji.location_sdk.data

import android.location.Location
import kotlinx.coroutines.flow.Flow

interface LocationUpdateCallback {
    fun onLocationUpdate(interval: Long) : Flow<Triple<Boolean, Long,Location>>
    class locationException(message: String) : Exception()
}
