package com.nonton.app.media

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

class NontonMediaController(
    private val context: Context,
    private val exoPlayer: ExoPlayer
) {
    init {
        exoPlayer.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.CONTENT_TYPE_MOVIE)
                .build(),
            true
        )
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) = Unit
        })
    }

    fun play(
        mediaId: String,
        mediaType: String,
        title: String,
        posterUrl: String?,
        videoUrl: String
    ) {
        exoPlayer.setMediaItem(buildMediaItem(videoUrl, title, posterUrl))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    fun pause() {
        exoPlayer.pause()
    }

    fun resume() {
        exoPlayer.playWhenReady = true
    }

    fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
    }

    fun setPlaybackSpeed(speed: Float) {
        exoPlayer.playbackParameters = exoPlayer.playbackParameters.withSpeed(speed)
    }

    private fun buildMediaItem(url: String, title: String, posterUrl: String?): MediaItem {
        val metadataBuilder = MediaMetadata.Builder().setTitle(title)
        posterUrl?.takeIf { it.isNotBlank() }?.let {
            metadataBuilder.setArtworkUri(Uri.parse(it))
        }

        val mimeType = when {
            url.contains(".m3u8", ignoreCase = true) -> MimeTypes.APPLICATION_M3U8
            url.contains(".mpd", ignoreCase = true) -> MimeTypes.APPLICATION_MPD
            else -> MimeTypes.VIDEO_MP4
        }

        return MediaItem.Builder()
            .setUri(url)
            .setMimeType(mimeType)
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }

    fun getCurrentPosition(): Long = exoPlayer.currentPosition

    fun getDuration(): Long = exoPlayer.duration

    fun isPlaying(): Boolean =
        exoPlayer.playWhenReady && exoPlayer.playbackState == Player.STATE_READY

    fun release() {
        exoPlayer.release()
    }
}

fun ExoPlayer.playMedia(url: String, title: String, posterUrl: String? = null) {
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .apply {
            posterUrl?.takeIf { it.isNotBlank() }?.let {
                setArtworkUri(Uri.parse(it))
            }
        }
        .build()

    val itemBuilder = MediaItem.Builder()
        .setUri(url)
        .setMediaMetadata(metadata)
    when {
        url.contains(".m3u8", ignoreCase = true) ->
            itemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
        url.contains(".mpd", ignoreCase = true) ->
            itemBuilder.setMimeType(MimeTypes.APPLICATION_MPD)
        else -> itemBuilder.setMimeType(MimeTypes.VIDEO_MP4)
    }

    setMediaItem(itemBuilder.build())
    prepare()
    playWhenReady = true
}
