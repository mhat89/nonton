package com.nonton.app.ui.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.nonton.app.R
import com.nonton.app.data.model.Media
import com.nonton.app.data.model.MediaCategory
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterialApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onMediaClick: (Media) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onProviderSettingsClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()
    val drawerIsOpen = drawerState.isOpen

    val mediaRows = listOf(
        "Trending Film" to MediaCategory.POPULAR,
        "Series Populer" to MediaCategory.POPULAR_SERIES,
        "Drakor Terbaru" to MediaCategory.POPULAR_DRAKOR,
        "Anime Populer" to MediaCategory.POPULAR_ANIME
    )
    val orderedRows = listOf(mediaRows[uiState.selectedTabIndex]) + mediaRows.filterIndexed { index, _ ->
        index != uiState.selectedTabIndex
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            NontonDrawer(
                providerStatus = uiState.providerStatus,
                onClose = { drawerScope.launch { drawerState.close() } },
                onHomeClick = {
                    viewModel.selectTab(0)
                    drawerScope.launch { drawerState.close() }
                },
                onSearchClick = {
                    drawerScope.launch { drawerState.close() }
                    onSearchClick()
                },
                onCategoryClick = { index ->
                    viewModel.selectTab(index)
                    drawerScope.launch { drawerState.close() }
                },
                onSettingsClick = {
                    drawerScope.launch { drawerState.close() }
                    onSettingsClick()
                },
                onProviderSettingsClick = {
                    drawerScope.launch { drawerState.close() }
                    onProviderSettingsClick()
                },
                onSyncClick = {
                    drawerScope.launch { drawerState.close() }
                    viewModel.syncProvider()
                }
            )
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                NontonTopBar(
                    drawerIsOpen = drawerIsOpen,
                    isSyncing = uiState.syncState.status == SyncStatus.Loading,
                    onMenuClick = {
                        if (drawerIsOpen) {
                            drawerScope.launch { drawerState.close() }
                        } else {
                            drawerScope.launch { drawerState.open() }
                        }
                    },
                    onSyncClick = viewModel::syncProvider
                )
            },
            bottomBar = {
                BottomNavBar(
                    selectedIndex = uiState.selectedTabIndex,
                    onItemClick = { index ->
                        when (index) {
                            0 -> viewModel.selectTab(0)
                            1 -> onSearchClick()
                            2 -> drawerScope.launch { drawerState.open() }
                            3 -> onSettingsClick()
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                val pullRefreshState = rememberPullRefreshState(
                    refreshing = uiState.isRefreshing,
                    onRefresh = viewModel::refresh
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pullRefresh(state = pullRefreshState)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            start = 16.dp,
                            top = 12.dp,
                            end = 16.dp,
                            bottom = 28.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        uiState.heroBanner?.let { hero ->
                            item(key = "hero-${hero.id}") {
                                HeroBanner(
                                    media = hero,
                                    onPlayClick = { onMediaClick(hero) },
                                    onSaveClick = { }
                                )
                            }
                        }

                        item(key = "category-tabs") {
                            CategoryTabs(
                                selectedIndex = uiState.selectedTabIndex,
                                onTabClick = viewModel::selectTab
                            )
                        }

                        orderedRows.forEach { (title, category) ->
                            val media = uiState.categoryResults[category]?.getOrNull()?.results.orEmpty()
                            if (media.isNotEmpty()) {
                                item(key = "row-${category.name}") {
                                    MediaRow(
                                        title = title,
                                        mediaList = media,
                                        onMediaClick = onMediaClick
                                    )
                                }
                            }
                        }

                        if (uiState.isRefreshing && uiState.categoryResults.isEmpty()) {
                            item(key = "home-shimmer") {
                                ShimmerEffect(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp),
                                    baseColor = MaterialTheme.colorScheme.surfaceVariant,
                                    highlightColor = MaterialTheme.colorScheme.surface
                                )
                            }
                        }

                        val hasContent = mediaRows.any { (title, category) ->
                            title.isNotBlank() && !uiState.categoryResults[category]?.getOrNull()?.results.isNullOrEmpty()
                        }
                        if (!hasContent && !uiState.isRefreshing) {
                            item(key = "empty-content") {
                                ContentUnavailable(
                                    providerStatus = uiState.providerStatus,
                                    message = uiState.error,
                                    onRetry = viewModel::syncProvider,
                                    onConfigureProvider = onProviderSettingsClick
                                )
                            }
                        }
                    }

                    PullRefreshIndicator(
                        refreshing = uiState.isRefreshing,
                        state = pullRefreshState,
                        modifier = Modifier.align(Alignment.TopCenter),
                        backgroundColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    ProviderSyncDialog(
        state = uiState.syncState,
        onDismiss = viewModel::dismissSyncState,
        onRetry = viewModel::syncProvider,
        onConfigureProvider = onProviderSettingsClick
    )
}

@Composable
private fun NontonTopBar(
    drawerIsOpen: Boolean,
    isSyncing: Boolean,
    onMenuClick: () -> Unit,
    onSyncClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.98f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onMenuClick) {
                Icon(
                    painter = painterResource(
                        if (drawerIsOpen) R.drawable.ic_close else R.drawable.ic_menu
                    ),
                    contentDescription = if (drawerIsOpen) "Tutup menu" else "Buka menu",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Text(
                text = "NONTON",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.8.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            IconButton(
                onClick = onSyncClick,
                enabled = !isSyncing
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_sync),
                        contentDescription = "Sinkronkan provider",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NontonDrawer(
    providerStatus: MainViewModel.ProviderStatus,
    onClose: () -> Unit,
    onHomeClick: () -> Unit,
    onSearchClick: () -> Unit,
    onCategoryClick: (Int) -> Unit,
    onSettingsClick: () -> Unit,
    onProviderSettingsClick: () -> Unit,
    onSyncClick: () -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier
            .width(320.dp)
            .fillMaxHeight(),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        windowInsets = WindowInsets(0, 0, 0, 0)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "NONTON",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.6.sp
                    )
                    Text(
                        text = "Streaming pilihan Anda",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = "Tutup menu",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                when (providerStatus) {
                                    MainViewModel.ProviderStatus.Ready -> Color(0xFF22C55E)
                                    MainViewModel.ProviderStatus.Error -> MaterialTheme.colorScheme.error
                                    MainViewModel.ProviderStatus.NotConfigured -> MaterialTheme.colorScheme.primary
                                    MainViewModel.ProviderStatus.Loading -> MaterialTheme.colorScheme.tertiary
                                }
                            )
                    )
                    Column(modifier = Modifier.padding(start = 10.dp)) {
                        Text(
                            text = "Status provider",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = when (providerStatus) {
                                MainViewModel.ProviderStatus.Ready -> "Aktif"
                                MainViewModel.ProviderStatus.Error -> "Perlu diperiksa"
                                MainViewModel.ProviderStatus.NotConfigured -> "Belum diatur"
                                MainViewModel.ProviderStatus.Loading -> "Memuat"
                            },
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            DrawerMenuItem("Beranda", "⌂", onHomeClick)
            DrawerMenuItem("Cari", "⌕", onSearchClick)
            DrawerMenuItem("Film", "▶", { onCategoryClick(0) })
            DrawerMenuItem("Series", "▣", { onCategoryClick(1) })
            DrawerMenuItem("Drakor", "◉", { onCategoryClick(2) })
            DrawerMenuItem("Anime", "✦", { onCategoryClick(3) })

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )

            DrawerMenuItem("Sinkronisasi Provider", "↻", onSyncClick)
            DrawerMenuItem("Kelola Provider", "⚙", onProviderSettingsClick)
            DrawerMenuItem("Pengaturan", "☷", onSettingsClick)

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Gunakan provider yang sah dan Anda miliki izin aksesnya.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start
            )
            Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun DrawerMenuItem(
    label: String,
    icon: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(30.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = label,
            modifier = Modifier.padding(start = 10.dp),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CategoryTabs(
    selectedIndex: Int,
    onTabClick: (Int) -> Unit
) {
    val categories = listOf("Film", "Series", "Drakor", "Anime")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEachIndexed { index, category ->
            val selected = selectedIndex == index
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { onTabClick(index) },
                shape = RoundedCornerShape(50),
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Text(
                    text = category,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
private fun ContentUnavailable(
    providerStatus: MainViewModel.ProviderStatus,
    message: String?,
    onRetry: () -> Unit,
    onConfigureProvider: () -> Unit
) {
    val needsConfiguration = providerStatus == MainViewModel.ProviderStatus.NotConfigured
    val title = when {
        needsConfiguration -> "Belum ada provider aktif"
        providerStatus == MainViewModel.ProviderStatus.Error -> "Konten belum dapat dimuat"
        else -> "Belum ada konten"
    }
    val description = when {
        needsConfiguration ->
            "Tambahkan URL JSON provider atau Base URL di Pengaturan Provider, lalu sinkronkan untuk memuat konten."
        !message.isNullOrBlank() -> message
        else -> "Sinkronkan provider untuk mengambil data terbaru."
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (needsConfiguration) "⚙" else "⚠",
            fontSize = 38.sp,
            color = if (needsConfiguration) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            }
        )
        Text(
            text = title,
            modifier = Modifier.padding(top = 10.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = description,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (needsConfiguration) {
            Button(onClick = onConfigureProvider) {
                Text("Tambah Provider")
            }
        } else {
            Button(onClick = onRetry) {
                Text("Coba Lagi")
            }
            TextButton(onClick = onConfigureProvider) {
                Text("Atur Provider")
            }
        }
    }
}

@Composable
fun MediaRow(
    title: String,
    mediaList: List<Media>,
    onMediaClick: (Media) -> Unit,
    modifier: Modifier = Modifier
) {
    if (mediaList.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "${mediaList.size} konten",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(mediaList, key = { it.id }) { media ->
                MediaPosterCard(
                    media = media,
                    onClick = { onMediaClick(media) }
                )
            }
        }
    }
}

@Composable
fun MediaPosterCard(
    media: Media,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(150.dp)
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        AsyncImage(
            model = media.posterPath,
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
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.92f))
                    )
                )
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = media.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (media.voteAverage > 0) {
                    Text(
                        text = "★ ${media.formattedRating}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )
                }
                Text(
                    text = media.year,
                    fontSize = 10.sp,
                    color = Color(0xFFB3B3B3)
                )
            }
        }
    }
}

@Composable
fun ProviderSyncDialog(
    state: ProviderSyncState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onConfigureProvider: () -> Unit
) {
    if (state.status == SyncStatus.Idle) return

    val isLoading = state.status == SyncStatus.Loading
    val isSuccess = state.status == SyncStatus.Success
    val needsConfiguration = state.status == SyncStatus.NeedsConfiguration

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Text(
                text = when (state.status) {
                    SyncStatus.Loading -> "Sinkronisasi Provider"
                    SyncStatus.Success -> "Sinkronisasi Berhasil"
                    SyncStatus.NeedsConfiguration -> "Provider Belum Diatur"
                    SyncStatus.Error -> "Sinkronisasi Gagal"
                    SyncStatus.Idle -> ""
                },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(42.dp))
                } else {
                    Text(
                        text = when {
                            isSuccess -> "✓"
                            needsConfiguration -> "⚙"
                            else -> "!"
                        },
                        style = MaterialTheme.typography.headlineLarge,
                        color = when {
                            isSuccess -> Color(0xFF22C55E)
                            needsConfiguration -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                }

                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )

                if (state.details.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            state.details.take(5).forEach { detail ->
                                Text(
                                    text = "• $detail",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            when {
                isLoading -> Unit
                needsConfiguration -> {
                    Button(
                        onClick = {
                            onDismiss()
                            onConfigureProvider()
                        }
                    ) {
                        Text("Tambah Provider")
                    }
                }
                isSuccess -> {
                    TextButton(onClick = onDismiss) {
                        Text("Tutup")
                    }
                }
                else -> {
                    TextButton(onClick = onRetry) {
                        Text("Coba Lagi")
                    }
                }
            }
        },
        dismissButton = {
            if (!isLoading && !needsConfiguration && !isSuccess) {
                TextButton(
                    onClick = {
                        onDismiss()
                        onConfigureProvider()
                    }
                ) {
                    Text("Atur Provider")
                }
            }
        }
    )
}
