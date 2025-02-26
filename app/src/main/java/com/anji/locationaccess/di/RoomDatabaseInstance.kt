package com.anji.locationaccess.di

import android.content.Context
import androidx.room.Room
import com.anji.locationaccess.data.local.DataImplementation
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object RoomDatabaseInstance {

    @Provides
    @Singleton
    fun provideRoomData(@ApplicationContext app: Context) =
        Room.databaseBuilder(app, DataImplementation::class.java, "user_data").build()
}