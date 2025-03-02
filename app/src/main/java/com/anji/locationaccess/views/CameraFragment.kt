package com.anji.locationaccess.views

import android.Manifest
import android.content.ContentResolver
import android.content.ContentValues
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.anji.location_sdk.data.LocationTagOnImage
import com.anji.location_sdk.viewmodel.MapViewModel
import com.anji.locationaccess.R
import com.anji.locationaccess.camera.data.model.CameraState
import com.anji.locationaccess.camera.data.viewmodel.CameraViewModel
import com.anji.locationaccess.data.model.ImageDetails
import com.anji.locationaccess.data.viewmodel.ImageViewModel
import com.anji.locationaccess.databinding.CameraLayoutBinding
import com.anji.locationaccess.util.AppConstants
import com.google.android.material.snackbar.Snackbar
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class CameraFragment : Fragment(), LocationTagOnImage.ImageListner {

    private lateinit var cameraLayoutBinding: CameraLayoutBinding
    private val permissions = mutableListOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
    ).apply {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.READ_MEDIA_IMAGES)
        }
    }
    private lateinit var locationTagOnImage: LocationTagOnImage
    private lateinit var overlayBitmap: Bitmap
    private lateinit var orginalPath: String
    private val cameraViewModel: CameraViewModel by viewModels<CameraViewModel>()
    private val imageViewModel: ImageViewModel by viewModels<ImageViewModel>()
    private val mapViewModel: MapViewModel by activityViewModels()
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var imageCapture: ImageCapture
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var camera: Camera
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var tempLocation: Location

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    private val permissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.all { it.value }) {
                onPermissionGranted()
            } else {
                view.let { v ->
                    showPermissionRationale()
                }
            }
        }

    private fun showPermissionRationale() {
        view?.let { v ->
            Snackbar.make(v, "Permissions are required to continue.", Snackbar.LENGTH_INDEFINITE)
                .setAction("Grant") {
                    requestPermissions(permissions.toTypedArray(), 1)
                }
                .show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        cameraLayoutBinding = CameraLayoutBinding.inflate(inflater, container, false)
        locationTagOnImage = LocationTagOnImage(requireContext())
        if (allPermissionsGranted()) {
            onPermissionGranted()
        } else {
            permissionRequest.launch(permissions.toTypedArray())
        }
        val cameraController = LifecycleCameraController(requireContext())
        cameraController.bindToLifecycle(viewLifecycleOwner)
        cameraLayoutBinding.btnTakePicture.bringToFront()
        cameraLayoutBinding.btnTakePicture.setOnClickListener {
            val cameraState = CameraState(controller = imageCapture)
            cameraViewModel.getImage(cameraState)
            cameraLayoutBinding.progressBar.visibility = View.VISIBLE
            cameraLayoutBinding.cameraLayout.visibility = View.GONE
            coroutineScope.launch {
                val state = cameraViewModel.cameraState.filter { !it.isLoading }
                    .first()
                withContext(Dispatchers.IO) {
                    if (state.bitmap != null) {
                        tempLocation = mapViewModel.locationFlow.first()?.third!!
                        locationTagOnImage.setTextSize(16f)
                        locationTagOnImage.setBackgroundRadius(5f)
                        locationTagOnImage.setBackgroundColor(Color.parseColor("#66000000"))
                        locationTagOnImage.setTextColor(Color.WHITE)
                        locationTagOnImage.setAuthorName("Anji")
                        locationTagOnImage.showAuthorName(true)
                        locationTagOnImage.setImageExtension(LocationTagOnImage.PNG)
                        locationTagOnImage.createImage(
                            tempLocation,
                            this@CameraFragment,
                            state.bitmap
                        )
                    }
                }
            }
        }
        cameraLayoutBinding.save.setOnClickListener {
            val userId = sharedPreferences.getLong(AppConstants.userId, 0L)
            val imageDetails = ImageDetails(
                imageId = userId, imagePath = orginalPath, longitude = tempLocation.longitude,
                latitude = tempLocation.latitude, timeStamp = System.currentTimeMillis()
            )
            imageViewModel.insertImage(imageDetails)
            coroutineScope.launch {
                val imageId: Long = imageViewModel.imageState.filter { !it.isLoading }.first().id!!
                if (imageId != 0L) {
                    withContext(Dispatchers.Main) {
                        requireActivity().supportFragmentManager.popBackStackImmediate()
                    }
                }
            }
        }
        return cameraLayoutBinding.root
    }

    private fun allPermissionsGranted() = permissions.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    private fun onPermissionGranted() {
        cameraLayoutBinding.previewView.let { vf ->
            vf.post {
                startCamera()
            }
        }
    }

    private val backCameraSelector by lazy {
        getCameraSelector(CameraSelector.LENS_FACING_BACK)
    }

    private fun getCameraSelector(lensFacing: Int) =
        CameraSelector.Builder().requireLensFacing(lensFacing).build()

    private fun startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider, backCameraSelector)
        }, ContextCompat.getMainExecutor(requireActivity()))
    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider, cameraSelector: CameraSelector) {
        val preview = Preview.Builder().build()

        preview.setSurfaceProvider(cameraLayoutBinding.previewView.surfaceProvider)

        imageCapture =
            ImageCapture.Builder()
                .setTargetRotation(cameraLayoutBinding.previewView.display.rotation).build()
        camera = cameraProvider.bindToLifecycle(
            viewLifecycleOwner,
            cameraSelector,
            preview,
            imageCapture
        )
    }

    private suspend fun saveImage(bitmap: Bitmap): String? {
        return withContext(Dispatchers.IO) {
            val resolver: ContentResolver = requireContext().contentResolver
            val imageCollection =
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val appName = requireContext().getString(R.string.app_name)
            val milliSec: Long = System.currentTimeMillis()
            val imageName = "${milliSec}_image.jpg"
            val imageContentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, imageName)
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + File.separator + appName
                )
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")
                put(MediaStore.MediaColumns.DATE_TAKEN, milliSec)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val imageUri: Uri? = resolver.insert(imageCollection, imageContentValues)
            imageUri?.let { uri ->
                try {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    }
                    imageContentValues.clear()
                    imageContentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, imageContentValues, null, null)

                    // ✅ Return the absolute path (for API 29+)
                    return@withContext getRealPathFromUri(uri)
                } catch (e: Exception) {
                    resolver.delete(uri, null, null)
                    return@withContext null
                }
            }
            return@withContext null
        }
    }

    private fun getRealPathFromUri(uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        requireContext().contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            if (cursor.moveToFirst()) {
                return cursor.getString(columnIndex)
            }
        }
        return uri.toString()
    }

    override fun getImagePath(path: String) {

    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }

    override fun getBitMap(bitMap: Bitmap) {
        coroutineScope.launch {
            withContext(Dispatchers.Main) {
                cameraLayoutBinding.progressBar.visibility = View.GONE
                cameraLayoutBinding.cameraLayout.visibility = View.GONE
                cameraLayoutBinding.imageViewParent.visibility = View.VISIBLE
                cameraLayoutBinding.cameraImage.setImageBitmap(bitMap)
                overlayBitmap = bitMap
                orginalPath = saveImage(bitMap)!!
            }
        }
    }
}