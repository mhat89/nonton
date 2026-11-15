package com.nonton.app.domain.usecase

import com.nonton.app.data.model.Media
import com.nonton.app.data.model.MediaCategory
import com.nonton.app.data.model.MediaListResponse
import com.nonton.app.data.model.MediaType
import com.nonton.app.data.model.SearchResponse
import com.nonton.app.data.model.VideoSource
import com.nonton.app.data.provider.DynamicMediaProvider
import com.nonton.app.data.provider.ProviderConfig
import com.nonton.app.data.provider.ProviderDownloadManager
import com.nonton.app.data.provider.ProviderDownloadResult
import com.nonton.app.data.repository.MediaRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class GetMediaListUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(
        category: MediaCategory = MediaCategory.POPULAR,
        page: Int = 1
    ): Result<MediaListResponse> = withContext(Dispatchers.IO) {
        when (category) {
            MediaCategory.POPULAR -> repository.getPopularMovies(page)
            MediaCategory.TOP_RATED -> repository.getTopRatedMovies(page)
            MediaCategory.NOW_PLAYING -> repository.getNowPlayingMovies(page)
            MediaCategory.POPULAR_SERIES -> repository.getPopularSeries(page)
            MediaCategory.TOP_RATED_SERIES -> repository.getTopRatedSeries(page)
            MediaCategory.ON_THE_AIR -> repository.getOnTheAirSeries(page)
            MediaCategory.POPULAR_ANIME -> repository.getPopularAnime(page)
            MediaCategory.POPULAR_DRAKOR -> repository.getPopularDrakor(page)
            MediaCategory.TRENDING_DAY -> repository.getTrending("day")
            MediaCategory.TRENDING_WEEK -> repository.getTrending("week")
        }
    }
}

@Singleton
class GetMediaDetailUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(mediaId: String, mediaType: MediaType): Result<Media> =
        withContext(Dispatchers.IO) {
            when (mediaType) {
                MediaType.MOVIE -> repository.getMovieDetail(mediaId)
                MediaType.TV_SERIES -> repository.getSeriesDetail(mediaId)
                MediaType.ANIME -> repository.getAnimeDetail(mediaId)
                MediaType.DRAKOR -> repository.getDrakorDetail(mediaId)
                MediaType.LIVE_TV -> Result.failure(
                    UnsupportedOperationException("Live TV detail is not implemented")
                )
            }
        }
}

@Singleton
class SearchMediaUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(
        query: String,
        page: Int = 1,
        type: SearchType = SearchType.MULTI
    ): Result<SearchResponse> = withContext(Dispatchers.IO) {
        when (type) {
            SearchType.MULTI -> repository.searchMulti(query, page)
            SearchType.MOVIES -> repository.searchMovies(query, page).map { it.toSearchResponse(query) }
            SearchType.SERIES -> repository.searchSeries(query, page).map { it.toSearchResponse(query) }
            SearchType.ANIME -> repository.searchAnime(query, page).map { it.toSearchResponse(query) }
        }
    }

    enum class SearchType {
        MULTI,
        MOVIES,
        SERIES,
        ANIME
    }
}

private fun MediaListResponse.toSearchResponse(query: String) = SearchResponse(
    page = page,
    results = results,
    totalPages = totalPages,
    totalResults = totalResults,
    query = query
)

@Singleton
class ResolveVideoSourceUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(mediaId: String): Result<VideoSource> =
        withContext(Dispatchers.IO) {
            repository.resolveVideoSource(mediaId)
        }
}

@Singleton
class SyncProviderUseCase @Inject constructor(
    private val downloadManager: ProviderDownloadManager,
    private val dynamicMediaProvider: DynamicMediaProvider
) {
    suspend operator fun invoke(): ProviderDownloadResult = withContext(Dispatchers.IO) {
        val result = downloadManager.downloadProviders()
        val activeProvider = dynamicMediaProvider.reloadActiveProvider()
        result.copy(
            config = activeProvider,
            activeProvider = activeProvider
        )
    }
}

/** Reads the currently selected provider without making a content request. */
@Singleton
class GetProviderStateUseCase @Inject constructor(
    private val dynamicMediaProvider: DynamicMediaProvider
) {
    suspend operator fun invoke(): ProviderConfig? = withContext(Dispatchers.IO) {
        dynamicMediaProvider.reloadActiveProvider()
    }
}

@Singleton
class GetWatchlistUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(): Result<List<Media>> = withContext(Dispatchers.IO) {
        repository.getWatchlist()
    }
}

@Singleton
class AddToWatchlistUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(media: Media): Result<Unit> = withContext(Dispatchers.IO) {
        repository.addToWatchlist(media)
    }
}

@Singleton
class RemoveFromWatchlistUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(mediaId: String): Result<Unit> = withContext(Dispatchers.IO) {
        repository.removeFromWatchlist(mediaId)
    }
}

@Singleton
class GetContinueWatchingUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(): Result<List<Media>> = withContext(Dispatchers.IO) {
        repository.getContinueWatching()
    }
}

@Singleton
class UpdatePlaybackPositionUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(
        mediaId: String,
        position: Long,
        duration: Long
    ): Result<Unit> = withContext(Dispatchers.IO) {
        repository.updatePlaybackPosition(mediaId, position, duration)
    }
}
