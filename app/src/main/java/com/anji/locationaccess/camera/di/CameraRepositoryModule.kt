package com.anji.locationaccess.camera.di

import com.anji.locationaccess.camera.data.repository.CameraRepositoryImpl
import com.anji.locationaccess.camera.domain.CameraRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CameraRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCameraRepo(cameraRepositoryImpl: CameraRepositoryImpl): CameraRepository

}