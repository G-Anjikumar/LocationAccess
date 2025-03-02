package com.anji.locationaccess.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.anji.locationaccess.data.model.ImageDetails
import com.anji.locationaccess.data.model.UserDetails

@Dao
interface ImageDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImageDetails(imageDetails: ImageDetails): Long

}