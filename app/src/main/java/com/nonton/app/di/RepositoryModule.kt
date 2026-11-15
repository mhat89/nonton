package com.nonton.app.di

import com.nonton.app.data.repository.MediaRepository
import com.nonton.app.data.repository.MediaRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMediaRepository(
        implementation: MediaRepositoryImpl
    ): MediaRepository
}
