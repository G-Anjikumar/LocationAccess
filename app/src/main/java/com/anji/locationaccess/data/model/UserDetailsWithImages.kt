package com.anji.locationaccess.data.model

import androidx.room.Embedded
import androidx.room.Relation
import androidx.room.TypeConverters
import com.anji.locationaccess.util.Convertors

@TypeConverters(Convertors::class)
data class UserDetailsWithImages(
    @Embedded
    private val userDetails:UserDetails,
    @Relation(
        parentColumn = "id",
        entityColumn = "imageId"
    )
    private val imageDetails: List<ImageDetails>
)

