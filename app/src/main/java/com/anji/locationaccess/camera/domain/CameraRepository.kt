package com.anji.locationaccess.camera.domain

import android.graphics.Bitmap
import androidx.camera.core.ImageCapture
import androidx.camera.view.LifecycleCameraController
import com.anji.locationaccess.data.model.Response

interface CameraRepository  {


    suspend fun captureImage(controller : ImageCapture) : Response<Bitmap>
}