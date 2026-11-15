package com.nonton.app.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import com.nonton.app.ui.theme.NontonTheme
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nonton.app.data.model.MediaType
import com.nonton.app.ui.detail.DetailScreen
import com.nonton.app.ui.player.PlayerScreen
import com.nonton.app.ui.search.SearchScreen
import com.nonton.app.ui.settings.ProviderSettingsScreen
import com.nonton.app.ui.settings.SettingsScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NontonTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NontonNavHost(viewModel = mainViewModel)
                }
            }
        }
    }
}

@Composable
private fun NontonNavHost(viewModel: MainViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            MainScreen(
                viewModel = viewModel,
                onMediaClick = { media ->
                    navController.navigate("detail/${media.id}/${media.mediaType.name}")
                },
                onSearchClick = { navController.navigate("search") },
                onSettingsClick = { navController.navigate("settings") },
                onProviderSettingsClick = { navController.navigate("provider_settings") }
            )
        }
        composable(
            route = "detail/{mediaId}/{mediaType}",
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
                onPlayClick = { media -> navController.navigate("player/${media.id}") }
            )
        }
        composable(
            route = "player/{mediaId}",
            arguments = listOf(navArgument("mediaId") { type = NavType.StringType })
        ) { entry ->
            PlayerScreen(mediaId = entry.arguments?.getString("mediaId").orEmpty())
        }
        composable("search") {
            SearchScreen(
                onMediaClick = { media ->
                    navController.navigate("detail/${media.id}/${media.mediaType.name}")
                }
            )
        }
        composable("settings") {
            SettingsScreen(
                onProviderSettingsClick = { navController.navigate("provider_settings") }
            )
        }
        composable("provider_settings") {
            ProviderSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
