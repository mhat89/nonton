package com.nonton.app.ui.player

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.nonton.app.data.model.VideoSource
import com.nonton.app.ui.main.ShimmerEffect
import com.nonton.app.ui.settings.ProviderSettingsActivity
import com.nonton.app.ui.theme.NontonTheme
import dagger.hilt.android.AndroidEntryPoint

@Composable
fun PlayerScreen(
    mediaId: String,
    videoUrl: String? = null,
    onProviderSettingsClick: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(mediaId, videoUrl) {
        viewModel.resolve(mediaId, videoUrl)
    }

    var playbackAttempt by remember(uiState.source?.url) { mutableIntStateOf(0) }
    var playerStatus by remember(uiState.source?.url) {
        mutableStateOf(PlayerStatus.Preparing)
    }
    var playbackError by remember(uiState.source?.url) { mutableStateOf<String?>(null) }

    val player = remember(uiState.source, playbackAttempt) {
        uiState.source?.let { source -> createPlayer(context, source) }
    }

    val listener = remember(uiState.source?.url) {
        object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                playerStatus = when (playbackState) {
                    Player.STATE_READY -> PlayerStatus.Ready
                    Player.STATE_ENDED -> PlayerStatus.Ended
                    else -> PlayerStatus.Preparing
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                playerStatus = PlayerStatus.Error
                playbackError = buildString {
                    append(error.errorCodeName)
                    error.localizedMessage?.takeIf { it.isNotBlank() }?.let {
                        append(": ")
                        append(it)
                    }
                }
            }
        }
    }

    DisposableEffect(player, listener) {
        player?.addListener(listener)
        player?.let { activePlayer ->
            when (activePlayer.playbackState) {
                Player.STATE_READY -> playerStatus = PlayerStatus.Ready
                Player.STATE_ENDED -> playerStatus = PlayerStatus.Ended
                else -> playerStatus = PlayerStatus.Preparing
            }
        }
        onDispose {
            player?.removeListener(listener)
            player?.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (player != null) {
            AndroidView(
                factory = { viewContext ->
                    PlayerView(viewContext).apply {
                        this.player = player
                        useController = true
                        controllerAutoShow = true
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        val showShimmer = uiState.isResolving ||
            (player != null && playerStatus == PlayerStatus.Preparing)

        if (showShimmer) {
            ShimmerEffect(
                modifier = Modifier.fillMaxSize(),
                baseColor = Color(0xFF111111),
                highlightColor = Color(0xFF292929)
            )
        }

        val visibleError = uiState.error ?: playbackError
        if (!uiState.isResolving && visibleError != null && playerStatus != PlayerStatus.Ready) {
            PlayerErrorPanel(
                message = visibleError,
                isMissingVideo = uiState.source == null,
                onRetry = {
                    playbackError = null
                    playerStatus = PlayerStatus.Preparing
                    playbackAttempt += 1
                    viewModel.resolve(mediaId, videoUrl)
                },
                onSyncAndRetry = {
                    playbackError = null
                    playerStatus = PlayerStatus.Preparing
                    playbackAttempt += 1
                    viewModel.syncAndRetry(mediaId)
                },
                onProviderSettingsClick = onProviderSettingsClick
            )
        }
    }
}

private fun createPlayer(
    context: android.content.Context,
    source: VideoSource
): ExoPlayer {
    val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setUserAgent("Mozilla/5.0 (Linux; Android 10; Mobile) Nonton/1.0")
        .setAllowCrossProtocolRedirects(true)
        .setDefaultRequestProperties(source.headers)
    val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
    val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

    return ExoPlayer.Builder(context)
        .setMediaSourceFactory(mediaSourceFactory)
        .build()
        .apply {
            setMediaItem(MediaItem.fromUri(source.url))
            prepare()
            playWhenReady = true
        }
}

@Composable
private fun PlayerErrorPanel(
    message: String,
    onRetry: () -> Unit,
    onSyncAndRetry: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Video tidak dapat diputar",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(onClick = onSyncAndRetry) {
                Text("Sinkronkan & Coba Lagi")
            }
            OutlinedButton(onClick = onRetry) {
                Text("Coba Lagi")
            }
        }
    }
}

private enum class PlayerStatus {
    Preparing,
    Ready,
    Ended,
    Error
}

@AndroidEntryPoint
class PlayerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mediaId = intent?.getStringExtra(EXTRA_MEDIA_ID).orEmpty()
        val videoUrl = intent?.data?.toString()

        setContent {
            NontonTheme {
                PlayerScreen(mediaId = mediaId, videoUrl = videoUrl)
            }
        }
    }

    companion object {
        const val EXTRA_MEDIA_ID = "media_id"
    }
}
