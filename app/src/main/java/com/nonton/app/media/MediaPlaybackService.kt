package com.nonton.app.media

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Hosts the background Media3 session. The session service owns the playback
 * notification, so the service only needs to keep the player and session in
 * sync with incoming commands.
 */
class MediaPlaybackService : MediaSessionService() {
    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        exoPlayer = ExoPlayer.Builder(this)
            .build()
            .apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.CONTENT_TYPE_MOVIE)
                        .build(),
                    true
                )
                setHandleAudioBecomingNoisy(true)
            }
        mediaSession = MediaSession.Builder(this, exoPlayer!!).build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val player = exoPlayer ?: return START_NOT_STICKY

        when (intent?.action) {
            ACTION_PLAY -> {
                val videoUrl = intent.getStringExtra(EXTRA_VIDEO_URL)
                if (!videoUrl.isNullOrBlank()) {
                    val metadata = MediaMetadata.Builder()
                        .setTitle(intent.getStringExtra(EXTRA_TITLE) ?: "Nonton")
                        .build()
                    player.setMediaItem(
                        MediaItem.Builder()
                            .setUri(videoUrl)
                            .setMediaMetadata(metadata)
                            .build()
                    )
                    player.prepare()
                    player.playWhenReady = true
                }
            }
            ACTION_PAUSE -> player.pause()
            ACTION_RESUME -> player.playWhenReady = true
            ACTION_REWIND -> player.seekTo((player.currentPosition - REWIND_MS).coerceAtLeast(0L))
            ACTION_FORWARD -> player.seekTo(player.currentPosition + FORWARD_MS)
            ACTION_STOP -> {
                player.stop()
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        exoPlayer?.release()
        exoPlayer = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_PLAY = "com.nonton.app.action.PLAY"
        const val ACTION_PAUSE = "com.nonton.app.action.PAUSE"
        const val ACTION_RESUME = "com.nonton.app.action.RESUME"
        const val ACTION_REWIND = "com.nonton.app.action.REWIND"
        const val ACTION_FORWARD = "com.nonton.app.action.FORWARD"
        const val ACTION_STOP = "com.nonton.app.action.STOP"
        const val EXTRA_MEDIA_ID = "media_id"
        const val EXTRA_VIDEO_URL = "video_url"
        const val EXTRA_TITLE = "title"

        private const val REWIND_MS = 10_000L
        private const val FORWARD_MS = 30_000L

        fun startPlayback(
            context: Context,
            mediaId: String,
            videoUrl: String,
            title: String
        ) {
            val intent = Intent(context, MediaPlaybackService::class.java).apply {
                action = ACTION_PLAY
                putExtra(EXTRA_MEDIA_ID, mediaId)
                putExtra(EXTRA_VIDEO_URL, videoUrl)
                putExtra(EXTRA_TITLE, title)
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
