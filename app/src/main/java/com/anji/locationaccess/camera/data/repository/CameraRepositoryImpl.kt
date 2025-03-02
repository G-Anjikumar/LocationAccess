package com.anji.locationaccess.camera.data.repository

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.core.content.ContextCompat
import com.anji.locationaccess.camera.domain.CameraRepository
import com.anji.locationaccess.data.model.Response
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class CameraRepositoryImpl @Inject constructor(
    private val application: Application
) : CameraRepository {

    lateinit var bitmap: Bitmap
    override suspend fun captureImage(controller: ImageCapture): Response<Bitmap> {
        return suspendCoroutine { continuation ->
            controller.takePicture(
                ContextCompat.getMainExecutor(application),
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        super.onCaptureSuccess(image)
                        val matrix = Matrix().apply {
                            postRotate(image.imageInfo.rotationDegrees.toFloat())
                        }
                        val bitmap = Bitmap.createBitmap(
                            image.toBitmap(),
                            0, 0,
                            image.width, image.height,
                            matrix, true
                        )
                        image.close()  // Ensure the image is closed to free resources
                        continuation.resume(Response.Success(bitmap))
                    }

                    override fun onError(exception: ImageCaptureException) {
                        super.onError(exception)
                        continuation.resume(Response.Error("Unable to Capture: ${exception.message}"))
                    }
                }
            )
        }
    }
}