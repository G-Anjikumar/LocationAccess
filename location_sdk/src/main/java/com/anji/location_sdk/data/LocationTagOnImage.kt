package com.anji.location_sdk.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import com.anji.location_sdk.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import javax.net.ssl.HttpsURLConnection


class LocationTagOnImage(context: Context) {
    private var place = ""
    private var road = ""
    private var latlng = ""
    private var date = ""
    private var originalImageHeight = 0
    private var originalImageWidth = 0
    private val context: Context
    private var returnFile: File? = null
    private var bitmap: Bitmap? = null
    private var mapBitmap: Bitmap? = null
    private var addresses: List<Address>? = null
    private var IMAGE_EXTENSION = ".png"
    private var fileUri: Uri? = null
    private var tempBitmap: Bitmap? = null
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private lateinit var listner: ImageListner
    private var geocoder: Geocoder? = null
    private var latitude = 0.0
    private var longitude = 0.0
    private var textSize = 0f
    private var textTopMargin = 0f
    private var typeface: Typeface? = null
    private var radius = 0f
    private var backgroundColor = 0
    private var textColor = 0
    private var backgroundHeight = 0f
    private var backgroundLeft = 0f
    private var authorName: String? = null
    private var showAuthorName = false
    public var showAppName = false
    private var showLatLng = false
    private var showDate = false
    private var showGoogleMap = false
    private val elementsList = ArrayList<String>()
    private var mapHeight = 0
    private var mapWidth = 0
    private var bitmapWidth = 0
    private var bitmapHeight = 0
    private var apiKey: String? = null
    private var center: String? = null
    private var imageUrl: String? = null
    private var dimension: String? = null
    private lateinit var _location: Location
    private var markerUrl: String? = null
    private var imageQuality: String? = null
    private val executor: Executor = Executors.newSingleThreadExecutor()
    private val executorService = Executors.newSingleThreadExecutor()
    var width: Int = 0
    var height: Int = 0
    private val TAG = Companion::class.java.simpleName

    init {
        this.context = context
    }

    fun createImage(location: Location, _listner: ImageListner, bitmapTemp: Bitmap) {
        val metrics: DisplayMetrics = context.resources.displayMetrics
        listner = _listner
        width = metrics.widthPixels
        height = metrics.heightPixels
        if (fileUri == null) {
            Log.d("FileError", "Unable to Find Location")
        }
        _location = location
        this.fileUri = fileUri
        this.tempBitmap = bitmapTemp

        // set default values here.
        textSize = 16f
        typeface = Typeface.DEFAULT
        radius = dpToPx(6f)
        backgroundColor = Color.parseColor("#66000000")
        textColor = context.getColor(android.R.color.white)
        backgroundHeight = 150f
        authorName = ""
        showAuthorName = false
        showAppName = false
        showGoogleMap = true
        showLatLng = true
        showDate = true
        mapHeight = backgroundHeight.toInt()
        mapWidth = 120
        imageQuality = null
        initialization()
    }

    private fun initialization() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
        if (imageQuality == null) {
            bitmapWidth = 960 * 2
            bitmapHeight = 1280 * 2
            backgroundHeight = (backgroundHeight * 2)
            mapWidth = 120 * 2
            mapHeight = backgroundHeight.toInt()
            textSize *= 2
            textTopMargin = (50 * 2).toFloat()
            radius *= 2
        }
        tempMethod()
        //        getDimensions();
    }

    fun tempMethod() {
        if (_location != null) {
            latitude = _location.latitude
            longitude = _location.longitude
            geocoder = Geocoder(context, Locale.getDefault())
            try {
                addresses = geocoder!!.getFromLocation(latitude, longitude, 1)
                apiKey = context.resources.getString(R.string.mapAPIKey)
                center = "$latitude,$longitude"
                dimension = mapWidth.toString() + "x" + mapHeight
                markerUrl = String.format(
                    Locale.getDefault(),
                    "%s%s%s",
                    "markers=color:red%7C",
                    center,
                    "&"
                )
                imageUrl = String.format(
                    Locale.getDefault(),
                    "https://maps.googleapis.com/maps/api/staticmap?center=%s&zoom=%d&size=%s&%s&maptype=%s&key=%s",
                    center,
                    15,
                    dimension,
                    markerUrl,
                    "satellite",
                    apiKey
                )
                executorService.submit(LoadImageTask(imageUrl!!))
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("error ", "${e.message}")
            }
        }
    }

    private inner class LoadImageTask(private val imageUrl: String) : Runnable {
        override fun run() {
            try {
                val bitmap = loadImageFromUrl(imageUrl)
                if (bitmap != null) {
                    mapBitmap = bitmap
                    val newBitmap = createBitmap()
                    listner.getBitMap(newBitmap)
                    storeBitmapInternally(newBitmap)
                    Handler(Looper.getMainLooper()).post {
                        imagePath()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadImageFromUrl(imageUrl: String): Bitmap? {
        return try {
            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpsURLConnection
            connection.doInput = true
            connection.connect()

            connection.inputStream.use { inputStream ->
                return BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun createBitmap(): Bitmap {
        val b =
            Bitmap.createBitmap(tempBitmap!!.width, tempBitmap!!.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(b)
        canvas.drawARGB(0, 255, 255, 255)
        canvas.drawRGB(255, 255, 255)
        copyTheImage(canvas)
        return b
    }

    fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    fun flipImage(source: Bitmap, horizontal: Boolean, vertical: Boolean): Bitmap {
        val matrix = Matrix()
        matrix.preScale(if (horizontal) -1f else 1f, if (vertical) -1f else 1f)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    fun getScreenDimensions(context: Context): Pair<Int, Int> {
        val metrics = context.resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        return Pair(width, height)
    }

    private fun copyTheImage(canvas: Canvas) {
        try {
            bitmap = tempBitmap
            Log.d("Bitmap_Image", "issue $bitmap")
        } catch (e: IOException) {
            e.printStackTrace()
        }
        val design = Paint()
        val bitmapWidth = bitmap!!.width
        val bitmapHeight = bitmap!!.height


        val targetWidth = canvas.width
        val targetHeight = canvas.height
        val scale = targetWidth.toFloat() / bitmapWidth

        val scaledHeight = (bitmapHeight * scale).toInt()
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap!!, targetWidth, scaledHeight, true)
        val yOffset = if (scaledHeight > targetHeight) {
            (scaledHeight - targetHeight) / 2f
        } else {
            0f
        }
        canvas.drawBitmap(scaledBitmap, 0f, -yOffset, design)
        val rectPaint = Paint()
        rectPaint.color = backgroundColor
        rectPaint.style = Paint.Style.FILL
        if (showAuthorName) {
            backgroundHeight += textTopMargin
        }
        if (showDate) {
            backgroundHeight += textTopMargin
        }
        if (showLatLng) {
            backgroundHeight += textTopMargin
        }
        if (mapBitmap != null) {
            Log.d("ImageDrawError", "mapBitmap Inside:: " + showGoogleMap)
            if (showGoogleMap) {
                Log.d("ImageDrawError", "showGoogleMap Inside:: ")
                val mapLeft = 10f
                backgroundLeft = (mapBitmap!!.width + 20).toFloat()
                canvas.drawRoundRect(
                    backgroundLeft,
                    canvas.height - backgroundHeight,
                    (canvas.width - 10).toFloat(),
                    (canvas.height - 10).toFloat(),
                    dpToPx(radius),
                    dpToPx(radius),
                    rectPaint
                )
                val scaledbmp2 = Bitmap.createScaledBitmap(
                    mapBitmap!!, mapWidth, mapHeight, false
                )
                canvas.drawBitmap(
                    scaledbmp2,
                    mapLeft,
                    canvas.height - backgroundHeight + (backgroundHeight - mapBitmap!!.height) / 2,
                    design
                )
                val textX = backgroundLeft + 10
                val textY = canvas.height - (backgroundHeight - textTopMargin)
                drawText(textX, textY, canvas)
            } else {
                backgroundLeft = 10f
                canvas.drawRoundRect(
                    backgroundLeft,
                    canvas.height - backgroundHeight,
                    (canvas.width - 10).toFloat(),
                    (canvas.height - 10).toFloat(),
                    dpToPx(radius),
                    dpToPx(radius),
                    rectPaint
                )
                val textX = backgroundLeft + 10
                val textY = canvas.height - (backgroundHeight - textTopMargin)
                drawText(textX, textY, canvas)
            }
        }
    }

    private fun drawText(textX: Float, textYDir: Float, canvas: Canvas) {
        var textY = textYDir
        if (imageQuality == null) {
            textSize *= 2
            textTopMargin = (50 * 2).toFloat()
        }
        elementsList.clear()
        val textPaint = Paint()
        textPaint.color = textColor
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.setTypeface(typeface)
        textPaint.textSize = textSize
        if (addresses != null) {
            place =
                addresses!![0].locality + ", " + addresses!![0].adminArea + ", " + addresses!![0].countryName
            road = addresses!![0].getAddressLine(0)
            elementsList.add(place)
            elementsList.add(road)
            if (showLatLng) {
                latlng = "Lat Lng : $latitude, $longitude"
                elementsList.add(latlng)
            }
        }
        if (showDate) {
            date = SimpleDateFormat("dd/MM/yyyy hh:mm a z", Locale.getDefault()).format(Date())
            elementsList.add(date)
        }
        if (showAuthorName) {
            authorName = "Clicked by : $authorName"
            elementsList.add(authorName!!)
        }
        for (item in elementsList) {
            canvas.drawText(item, textX, textY, textPaint)
            textY += textTopMargin
        }
        if (showAppName) {
            val packageManager = context.packageManager
            val applicationInfo: ApplicationInfo = try {
                packageManager.getApplicationInfo(context.applicationInfo.packageName, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                return e.printStackTrace()
            }
            val appName = packageManager.getApplicationLabel(applicationInfo)
            if (imageQuality != null) {
                textSize /= 2
                textTopMargin = (50 * 3.6).toFloat()
                textPaint.textSize = textSize
                textY = (canvas.height - 40).toFloat()
                canvas.drawText(
                    appName.toString(),
                    canvas.width - 10 - 20 - textPaint.measureText(appName.toString()),
                    textY,
                    textPaint
                )
            } else {
                textSize /= 2
                textTopMargin = (50 * 2).toFloat()
                textY = (canvas.height - 20).toFloat()
                textPaint.textSize = textSize
                canvas.drawText(
                    appName.toString(),
                    canvas.width - 10 - 10 - textPaint.measureText(appName.toString()),
                    textY,
                    textPaint
                )
            }
        }
    }

    private fun dpToPx(dp: Float): Float {
        return dp * context.resources.displayMetrics.density
    }

    private fun storeBitmapInternally(b: Bitmap) {
        val pictureFile = outputMediaFile()
        returnFile = pictureFile
        if (pictureFile == null) {
            Log.e(TAG, "Error creating media file, check storage permissions: ")
            return
        }
        try {
            val outputStream = ByteArrayOutputStream()
            b.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            val compressedImageData = outputStream.toByteArray()
            val fileOutputStream = FileOutputStream(pictureFile)
            fileOutputStream.write(compressedImageData)
            fileOutputStream.close()
        } catch (e: IOException) {
            Log.e(TAG, "${e.message}")
            e.printStackTrace()
        }
    }

    private fun outputMediaFile(): File? {
        val mediaStorageDir = File(
            (Environment.getExternalStorageDirectory()
                .toString() + "/Android/data/"
                    + context.applicationContext.packageName
                    ) + "/files"
        )
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null
            }
        }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        val mImageName = "IMG_$timeStamp$IMAGE_EXTENSION"
        val imagePath = mediaStorageDir.path + File.separator + mImageName
        val mediaFile = File(imagePath)
        MediaScannerConnection.scanFile(
            context, arrayOf(imagePath), null
        ) { path, uri -> }
        return mediaFile
    }

    fun setTextSize(textSize: Float) {
        this.textSize = textSize
    }

    fun setCustomFont(typeface: Typeface?) {
        this.typeface = typeface
    }

    fun setBackgroundRadius(radius: Float) {
        this.radius = radius
    }

    fun setBackgroundColor(backgroundColor: Int) {
        this.backgroundColor = backgroundColor
    }

    fun setTextColor(textColor: Int) {
        this.textColor = textColor
    }

    fun showAuthorName(showAuthorName: Boolean) {
        this.showAuthorName = showAuthorName
    }

    fun setAuthorName(authorName: String?) {
        this.authorName = authorName
    }

    fun setImageExtension(imgExtension: String) {
        IMAGE_EXTENSION = imgExtension
        when (imgExtension) {
            JPG -> IMAGE_EXTENSION = ".jpg"
            PNG -> IMAGE_EXTENSION = ".png"
            JPEG -> IMAGE_EXTENSION = ".jpeg"
        }
    }

    fun imagePath(): String? {
        val mediaStorageDir = File(
            (Environment.getExternalStorageDirectory()
                .toString() + "/Android/data/"
                    + context.applicationContext.packageName
                    ) + "/files"
        )
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null
            }
        }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        val mImageName = "IMG_$timeStamp$IMAGE_EXTENSION"
        val ImagePath = mediaStorageDir.path + File.separator + mImageName
        val media = File(ImagePath)
        MediaScannerConnection.scanFile(
            context, arrayOf(media.absolutePath), null
        ) { path, uri -> }
        listner.getImagePath(ImagePath)
        return ImagePath
    }

    companion object {
        const val PNG = ".png"
        const val JPG = ".jpg"
        const val JPEG = ".jpeg"
    }

    interface ImageListner {
        fun getImagePath(path: String)

        fun getBitMap(bitMap: Bitmap)
    }
}
