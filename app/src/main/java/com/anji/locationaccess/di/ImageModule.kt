package com.anji.locationaccess.di

import com.anji.locationaccess.data.local.repo.image.ImageRepository
import com.anji.locationaccess.data.local.repo.image.ImageRepositoryImpl
import com.anji.locationaccess.data.local.repo.user.UserRepository
import com.anji.locationaccess.data.local.repo.user.UserRepositoryImplementation
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ImageModule {

    @Binds
    @Singleton
    abstract fun provideUserRepo(imageRepositoryImpl: ImageRepositoryImpl) : ImageRepository
}