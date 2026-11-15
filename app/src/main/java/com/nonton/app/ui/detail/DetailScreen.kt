package com.nonton.app.ui.detail

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import coil.compose.AsyncImage
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nonton.app.data.model.Media
import com.nonton.app.data.model.MediaType
import com.nonton.app.domain.usecase.GetMediaDetailUseCase
import com.nonton.app.ui.theme.NontonTheme
import com.nonton.app.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val getMediaDetail: GetMediaDetailUseCase
) : ViewModel() {
    private val _media = MutableStateFlow<Result<Media>?>(null)
    val media = _media.asStateFlow()

    fun load(mediaId: String, mediaType: MediaType) {
        if (mediaId.isBlank() || _media.value != null) return
        viewModelScope.launch {
            _media.value = getMediaDetail(mediaId, mediaType)
        }
    }
}

@Composable
fun DetailScreen(
    mediaId: String,
    mediaType: MediaType,
    onPlayClick: (Media) -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val mediaResult by viewModel.media.collectAsState()
    LaunchedEffect(mediaId, mediaType) {
        viewModel.load(mediaId, mediaType)
    }

    NontonTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when {
                mediaResult == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                mediaResult?.isFailure == true -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mediaResult?.exceptionOrNull()?.message ?: "Gagal memuat detail",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                else -> {
                    val media = mediaResult?.getOrNull()
                    if (media == null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Konten tidak ditemukan")
                        }
                    } else {
                        DetailContent(
                            media = media,
                            onPlayClick = onPlayClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailContent(
    media: Media,
    onPlayClick: (Media) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // Backdrop image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        ) {
            AsyncImage(
                model = media.posterPath,
                contentDescription = media.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomStart
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0xFF0D0D0D).copy(alpha = 0.95f)
                                    )
                                )
                            )
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title and meta
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "★ ${media.formattedRating}",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFFFD700)
                )
                Text(
                    text = media.year,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFB3B3B3)
                )
            }

            Text(
                text = media.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = media.overview ?: "Deskripsi tidak tersedia.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )

            // Play button
            Button(
                onClick = { onPlayClick(media) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Putar Sekarang")
            }
        }
    }
}

@AndroidEntryPoint
class DetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mediaId = intent?.getStringExtra(EXTRA_MEDIA_ID).orEmpty()
        val mediaType = intent?.getStringExtra(EXTRA_MEDIA_TYPE)
            ?.let { value -> runCatching { MediaType.valueOf(value) }.getOrNull() }
            ?: MediaType.MOVIE

        setContent {
            NontonTheme {
                DetailScreen(
                    mediaId = mediaId,
                    mediaType = mediaType,
                    onPlayClick = { finish() }
                )
            }
        }
    }

    companion object {
        const val EXTRA_MEDIA_ID = "media_id"
        const val EXTRA_MEDIA_TYPE = "media_type"
    }
}
