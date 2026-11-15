package com.nonton.app.ui.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.nonton.app.R
import com.nonton.app.data.model.Media
import com.nonton.app.data.model.MediaCategory
import com.nonton.app.data.model.MediaType
import com.nonton.app.ui.detail.DetailScreen
import com.nonton.app.ui.main.MainViewModel
import com.nonton.app.ui.main.ProviderSyncDialog
import com.nonton.app.ui.player.PlayerScreen
import com.nonton.app.ui.settings.ProviderSettingsScreen
import com.nonton.app.ui.theme.NontonTheme
import com.nonton.app.util.Constants
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TvMainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NontonTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TvNavHost(viewModel = mainViewModel)
                }
            }
        }
    }
}

@Composable
private fun TvNavHost(viewModel: MainViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "tv_home") {
        composable("tv_home") {
            TvMainScreen(
                viewModel = viewModel,
                onMediaClick = { media ->
                    navController.navigate("tv_detail/${media.id}/${media.mediaType.name}")
                },
                onProviderSettingsClick = { navController.navigate("tv_provider_settings") },
                onSyncClick = viewModel::syncProvider
            )
        }
        composable(
            route = "tv_detail/{mediaId}/{mediaType}",
            arguments = listOf(
                navArgument("mediaId") { type = NavType.StringType },
                navArgument("mediaType") { type = NavType.StringType }
            )
        ) { entry ->
            val mediaId = entry.arguments?.getString("mediaId").orEmpty()
            val mediaType = entry.arguments?.getString("mediaType")
                ?.let { value -> runCatching { MediaType.valueOf(value) }.getOrNull() }
                ?: MediaType.MOVIE
            DetailScreen(
                mediaId = mediaId,
                mediaType = mediaType,
                onPlayClick = { media -> navController.navigate("tv_player/${media.id}") }
            )
        }
        composable(
            route = "tv_player/{mediaId}",
            arguments = listOf(navArgument("mediaId") { type = NavType.StringType })
        ) { entry ->
            PlayerScreen(mediaId = entry.arguments?.getString("mediaId").orEmpty())
        }
        composable("tv_provider_settings") {
            ProviderSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun TvMainScreen(
    onMediaClick: (Media) -> Unit,
    viewModel: MainViewModel,
    onProviderSettingsClick: () -> Unit,
    onSyncClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories = listOf(
        "Trending Film" to MediaCategory.POPULAR,
        "Series Populer" to MediaCategory.POPULAR_SERIES,
        "Drakor Terbaru" to MediaCategory.POPULAR_DRAKOR,
        "Anime Populer" to MediaCategory.POPULAR_ANIME
    )
    val hasContent = categories.any { (_, category) ->
        uiState.categoryResults[category]?.getOrNull()?.results?.isNotEmpty() == true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(PaddingValues(start = 48.dp, top = 96.dp, end = 48.dp, bottom = 48.dp)),
            verticalArrangement = Arrangement.spacedBy(48.dp)
        ) {
            if (uiState.providerStatus == MainViewModel.ProviderStatus.NotConfigured) {
                item(key = "provider-not-configured") {
                    TvProviderUnavailable(
                        onConfigureProvider = onProviderSettingsClick
                    )
                }
            } else {
                item(key = "provider-actions") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onProviderSettingsClick) {
                            Text("Kelola Provider", fontSize = 18.sp)
                        }
                        Button(onClick = onSyncClick) {
                            Text("Sinkronkan Provider", fontSize = 18.sp)
                        }
                    }
                }
                item {
                    TvHeroBanner(
                        media = uiState.heroBanner,
                        onPlayClick = onMediaClick
                    )
                }
                items(categories, key = { it.second.name }) { (title, category) ->
                    TvMediaRow(
                        title = title,
                        mediaList = uiState.categoryResults[category]?.getOrNull()?.results.orEmpty(),
                        onMediaClick = onMediaClick
                    )
                }
                if (!hasContent && !uiState.isRefreshing) {
                    item(key = "content-error") {
                        Text(
                            text = uiState.error ?: "Belum ada konten dari provider.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 22.sp
                        )
                    }
                }
            }
        }

        ProviderSyncDialog(
            state = uiState.syncState,
            onDismiss = viewModel::dismissSyncState,
            onRetry = onSyncClick,
            onConfigureProvider = onProviderSettingsClick
        )
    }
}

@Composable
private fun TvProviderUnavailable(onConfigureProvider: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Belum ada provider aktif",
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Tambahkan URL JSON provider atau Base URL untuk mulai memuat konten.",
            fontSize = 22.sp,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Button(onClick = onConfigureProvider) {
            Text("Tambah Provider", fontSize = 20.sp)
        }
    }
}

@Composable
private fun TvHeroBanner(
    media: Media?,
    onPlayClick: (Media) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0F3460),
                        Color(0xFF16213E),
                        Color(0xFF1A1A2E)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(1f, 1f)
                )
            )
    ) {
        media?.let { currentMedia ->
            AsyncImage(
                model = currentMedia.posterPath,
                contentDescription = currentMedia.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = media?.title ?: "Nonton",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = media?.year?.takeIf { it.isNotBlank() }
                    ?: "Film, series, anime, dan drakor favorit Anda.",
                fontSize = 24.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
            media?.let { currentMedia ->
                Button(onClick = { onPlayClick(currentMedia) }) {
                    Text("Putar Sekarang", fontSize = 20.sp)
                }
            }
        }
    }
}

@Composable
fun TvMediaRow(
    title: String,
    mediaList: List<Media>,
    onMediaClick: (Media) -> Unit
) {
    if (mediaList.isEmpty()) return

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            items(mediaList, key = { it.id }) { media ->
                TvMediaCard(media = media, onClick = { onMediaClick(media) })
            }
        }
    }
}

@Composable
private fun TvMediaCard(
    media: Media,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(280.dp)
            .height(420.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .background(Color(0xFF1E1E1E))
    ) {
        AsyncImage(
            model = media.posterPath?.let { path ->
                if (path.startsWith("http://") || path.startsWith("https://")) {
                    path
                } else {
                    "${Constants.IMAGE_BASE_URL}${Constants.POSTER_SIZE_W500}$path"
                }
            },
            contentDescription = media.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            placeholder = painterResource(id = R.drawable.ic_movie_placeholder),
            error = painterResource(id = R.drawable.ic_movie_placeholder)
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                    )
                )
                .padding(16.dp)
        ) {
            Text(
                text = media.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "★ ${media.formattedRating} • ${media.year}",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}
