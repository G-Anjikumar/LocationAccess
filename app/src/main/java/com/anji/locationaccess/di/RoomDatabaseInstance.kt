package com.anji.locationaccess.di

import android.content.Context
import androidx.room.Room
import com.anji.locationaccess.R
import com.anji.locationaccess.data.local.dao.DataImplementation
import com.anji.locationaccess.data.local.dao.ImageDAO
import com.anji.locationaccess.data.local.dao.UserDAO
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object RoomDatabaseInstance {

    @Provides
    @Singleton
    fun provideRoomDatabase(@ApplicationContext app: Context): DataImplementation {
        val passphrase: ByteArray = net.sqlcipher.database.SQLiteDatabase.getBytes(R.string.secure_pass_room.toString().toCharArray())
        val factory = SupportFactory(passphrase)
        return Room.databaseBuilder(app, DataImplementation::class.java, "user_data")
            .openHelperFactory(factory)
            .allowMainThreadQueries().build()
    }


    @Provides
    @Singleton
    fun provideUserDao(database: DataImplementation): UserDAO {
        return database.userDao()
    }

    @Provides
    @Singleton
    fun provideImageDao(database: DataImplementation): ImageDAO {
        return database.imageDao()
    }

}
