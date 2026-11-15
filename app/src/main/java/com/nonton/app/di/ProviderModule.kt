package com.nonton.app.di

import android.content.Context
import com.nonton.app.data.provider.ConfigBasedProvider
import com.nonton.app.data.provider.DynamicMediaProvider
import com.nonton.app.data.provider.MediaProvider
import com.nonton.app.data.provider.ProviderDownloadManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ProviderModule {

    @Provides
    @Singleton
    fun provideConfigBasedProvider(): ConfigBasedProvider = ConfigBasedProvider()

    @Provides
    @Singleton
    fun provideProviderDownloadManager(
        @ApplicationContext context: Context
    ): ProviderDownloadManager = ProviderDownloadManager(context)

    @Provides
    @Singleton
    fun provideDynamicMediaProvider(
        configBasedProvider: ConfigBasedProvider,
        downloadManager: ProviderDownloadManager
    ): DynamicMediaProvider = DynamicMediaProvider(
        configBasedProvider = configBasedProvider,
        downloadManager = downloadManager
    )

    @Provides
    @Singleton
    fun provideMediaProvider(dynamicMediaProvider: DynamicMediaProvider): MediaProvider =
        dynamicMediaProvider
}
