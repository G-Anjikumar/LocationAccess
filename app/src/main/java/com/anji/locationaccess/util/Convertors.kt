package com.anji.locationaccess.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.room.TypeConverter
import com.anji.locationaccess.data.model.ImageDetails
import com.google.gson.Gson
import java.io.ByteArrayOutputStream

class Convertors {

    private val gson = Gson()

    @TypeConverter
    fun fromImageDetails(imageDetails: ImageDetails?): String? {
        return imageDetails?.let {
            Gson().toJson(it)
        }
    }

    @TypeConverter
    fun toImageDetails(json: String?): ImageDetails? {
        return json?.let {
            Gson().fromJson(it, ImageDetails::class.java)
        }
    }

    @TypeConverter
    fun fromBitmap(bitmap: Bitmap?): ByteArray? {
        return bitmap?.let {
            val stream = ByteArrayOutputStream()
            it.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.toByteArray()
        }
    }

    @TypeConverter
    fun toBitmap(byteArray: ByteArray?): Bitmap? {
        return byteArray?.let {
            BitmapFactory.decodeByteArray(it, 0, it.size)
        }
    }
}