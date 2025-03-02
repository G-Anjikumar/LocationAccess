package com.anji.locationaccess.data.model

import android.graphics.Bitmap
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.anji.locationaccess.util.Convertors

@Entity(
    tableName = "imageDetails",
    foreignKeys = [
        ForeignKey(
            entity = UserDetails::class,
            parentColumns = ["id"],
            childColumns = ["imageId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
@TypeConverters(Convertors::class)
data class ImageDetails(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var imageId: Long = 0,
    var image: Bitmap? = null,
    var imagePath: String? = null,
    var latitude: Double? = null,
    var longitude: Double? = null,
    var timeStamp: Long? = null
)