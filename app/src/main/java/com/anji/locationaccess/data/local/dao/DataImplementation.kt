package com.anji.locationaccess.data.local.dao

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.anji.locationaccess.data.local.dao.UserDAO
import com.anji.locationaccess.data.model.ImageDetails
import com.anji.locationaccess.data.model.UserDetails
import com.anji.locationaccess.util.Convertors

@Database(entities = [UserDetails::class,ImageDetails::class], version = 1, exportSchema = false)
@TypeConverters(Convertors::class)
abstract class DataImplementation : RoomDatabase() {

    abstract fun userDao(): UserDAO
    abstract fun imageDao(): ImageDAO
}
