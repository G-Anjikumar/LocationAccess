package com.anji.locationaccess.camera.data.model

import android.graphics.Bitmap
import androidx.camera.core.ImageCapture
import androidx.camera.view.LifecycleCameraController

data class CameraState(
    val isLoading: Boolean = false,
    val bitmap:Bitmap?=null,
    val error: String? = null,
    val controller : ImageCapture?=null
)