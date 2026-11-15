package com.nonton.app.di

import android.content.Context
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import com.nonton.app.media.NontonMediaController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {

    @Provides
    @Singleton
    fun provideDefaultTrackSelector(context: Context): DefaultTrackSelector =
        DefaultTrackSelector(context)

    @Provides
    @Singleton
    fun provideDefaultLoadControl(): DefaultLoadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            15_000,
            50_000,
            2_500,
            5_000
        )
        .setPrioritizeTimeOverSizeThresholds(true)
        .build()

    @Provides
    @Singleton
    fun provideExoPlayer(
        context: Context,
        trackSelector: DefaultTrackSelector,
        loadControl: DefaultLoadControl
    ): ExoPlayer = ExoPlayer.Builder(context)
        .setTrackSelector(trackSelector)
        .setLoadControl(loadControl)
        .setHandleAudioBecomingNoisy(true)
        .build()

    @Provides
    @Singleton
    fun providePlayerView(context: Context): PlayerView = PlayerView(context).apply {
        useController = true
        controllerShowTimeoutMs = 5_000
        controllerHideOnTouch = true
    }

    @Provides
    @Singleton
    fun provideNontonMediaController(
        context: Context,
        exoPlayer: ExoPlayer
    ): NontonMediaController = NontonMediaController(context, exoPlayer)
}
