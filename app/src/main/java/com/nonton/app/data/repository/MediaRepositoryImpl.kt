package com.nonton.app.data.repository

import com.nonton.app.data.model.CreditsResponse
import com.nonton.app.data.model.DownloadStatus
import com.nonton.app.data.model.GenreResponse
import com.nonton.app.data.model.HomePageResponse
import com.nonton.app.data.model.Media
import com.nonton.app.data.model.MediaCategory
import com.nonton.app.data.model.MediaListResponse
import com.nonton.app.data.model.MediaType
import com.nonton.app.data.model.ProviderResponse
import com.nonton.app.data.model.SearchResponse
import com.nonton.app.data.model.Season
import com.nonton.app.data.model.VideoSource
import com.nonton.app.data.provider.MediaProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class MediaRepositoryImpl @Inject constructor(
    private val mediaProvider: MediaProvider
) : MediaRepository {

    override suspend fun getHomePage(): Result<HomePageResponse> = withContext(Dispatchers.IO) {
        try {
            val popular = mediaProvider.getMediaList(MediaCategory.POPULAR, 1)
            Result.success(
                HomePageResponse(
                    continueWatching = popular.results.take(6),
                    trendingMovies = popular.results,
                    trendingSeries = popular.results.take(6),
                    popularDrakor = emptyList(),
                    popularAnime = emptyList(),
                    topRated = popular.results,
                    newReleases = popular.results,
                    liveChannels = emptyList(),
                    genres = emptyMap()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun categoryToResults(category: MediaCategory, page: Int): Result<MediaListResponse> {
        return try {
            val response = mediaProvider.getMediaList(category, page)
            Result.success(response)
        } catch (e: Exception) {
            // Provider failures must remain visible to the UI; never replace them with mock data.
            Result.failure(e)
        }
    }

    override suspend fun getPopularMovies(page: Int): Result<MediaListResponse> =
        categoryToResults(MediaCategory.POPULAR, page)

    override suspend fun getTopRatedMovies(page: Int): Result<MediaListResponse> =
        categoryToResults(MediaCategory.TOP_RATED, page)

    override suspend fun getNowPlayingMovies(page: Int): Result<MediaListResponse> =
        categoryToResults(MediaCategory.NOW_PLAYING, page)

    override suspend fun getMovieDetail(movieId: String): Result<Media> = withContext(Dispatchers.IO) {
        try {
            val media = mediaProvider.getMediaDetail(movieId)
            if (media != null) {
                Result.success(media)
            } else {
                Result.failure(Exception("Media tidak ditemukan"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resolveVideoSource(movieId: String): Result<VideoSource> =
        withContext(Dispatchers.IO) {
            try {
                val url = mediaProvider.getVideoUrl(movieId)
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?: return@withContext Result.failure(
                        IllegalStateException("Provider tidak menemukan URL video untuk konten ini")
                    )
                Result.success(
                    VideoSource(
                        url = url,
                        headers = mediaProvider.getVideoHeaders()
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun getSimilarMovies(movieId: String, page: Int): Result<MediaListResponse> =
        categoryToResults(MediaCategory.POPULAR, page)

    override suspend fun getMovieRecommendations(movieId: String, page: Int): Result<MediaListResponse> =
        categoryToResults(MediaCategory.POPULAR, page)

    override suspend fun getMovieCredits(movieId: String): Result<CreditsResponse> =
        Result.success(CreditsResponse(id = 0))

    override suspend fun getMovieProviders(movieId: String): Result<ProviderResponse> =
        Result.success(ProviderResponse())

    override suspend fun getPopularSeries(page: Int): Result<MediaListResponse> =
        categoryToResults(MediaCategory.POPULAR_SERIES, page)

    override suspend fun getTopRatedSeries(page: Int): Result<MediaListResponse> =
        categoryToResults(MediaCategory.TOP_RATED_SERIES, page)

    override suspend fun getOnTheAirSeries(page: Int): Result<MediaListResponse> =
        categoryToResults(MediaCategory.ON_THE_AIR, page)

    override suspend fun getSeriesDetail(seriesId: String): Result<Media> = withContext(Dispatchers.IO) {
        try {
            val media = mediaProvider.getMediaDetail(seriesId)
            if (media != null) {
                Result.success(media)
            } else {
                Result.failure(Exception("Series tidak ditemukan"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSeasonDetail(seriesId: String, seasonNumber: Int): Result<Season> =
        withContext(Dispatchers.IO) {
            Result.failure(Exception("Detail season belum diimplementasikan oleh provider"))
        }

    override suspend fun getSimilarSeries(seriesId: String, page: Int): Result<MediaListResponse> =
        categoryToResults(MediaCategory.POPULAR_SERIES, page)

    override suspend fun getSeriesRecommendations(seriesId: String, page: Int): Result<MediaListResponse> =
        categoryToResults(MediaCategory.POPULAR_SERIES, page)

    override suspend fun getSeriesCredits(seriesId: String): Result<CreditsResponse> =
        Result.success(CreditsResponse(id = 0))

    override suspend fun getSeriesProviders(seriesId: String): Result<ProviderResponse> =
        Result.success(ProviderResponse())

    override suspend fun getPopularAnime(page: Int): Result<MediaListResponse> =
        categoryToResults(MediaCategory.POPULAR_ANIME, page)

    override suspend fun getAnimeDetail(animeId: String): Result<Media> = withContext(Dispatchers.IO) {
        try {
            val media = mediaProvider.getMediaDetail(animeId)
            if (media != null) {
                Result.success(media)
            } else {
                Result.failure(Exception("Anime tidak ditemukan"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPopularDrakor(page: Int): Result<MediaListResponse> =
        categoryToResults(MediaCategory.POPULAR_DRAKOR, page)

    override suspend fun getDrakorDetail(drakorId: String): Result<Media> = withContext(Dispatchers.IO) {
        try {
            val media = mediaProvider.getMediaDetail(drakorId)
            if (media != null) {
                Result.success(media)
            } else {
                Result.failure(Exception("Drakor tidak ditemukan"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchMulti(query: String, page: Int): Result<SearchResponse> = withContext(Dispatchers.IO) {
        try {
            val response = mediaProvider.searchMedia(query, page)
            Result.success(
                SearchResponse(
                    page = response.page,
                    results = response.results,
                    totalPages = response.totalPages,
                    totalResults = response.totalResults,
                    query = query
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchMovies(query: String, page: Int): Result<MediaListResponse> = withContext(Dispatchers.IO) {
        try {
            Result.success(mediaProvider.searchMedia(query, page))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchSeries(query: String, page: Int): Result<MediaListResponse> = withContext(Dispatchers.IO) {
        try {
            Result.success(mediaProvider.searchMedia(query, page))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchAnime(query: String, page: Int): Result<MediaListResponse> = withContext(Dispatchers.IO) {
        try {
            Result.success(mediaProvider.searchMedia(query, page))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMovieGenres(): Result<GenreResponse> =
        Result.success(GenreResponse())

    override suspend fun getTvGenres(): Result<GenreResponse> =
        Result.success(GenreResponse())

    override suspend fun getTrending(timeWindow: String): Result<MediaListResponse> =
        categoryToResults(MediaCategory.TRENDING_DAY, 1)

    override suspend fun discoverMovies(
        page: Int,
        genres: String?,
        sortBy: String,
        minRating: Double,
        year: Int?
    ): Result<MediaListResponse> =
        categoryToResults(MediaCategory.POPULAR, page)

    override suspend fun discoverSeries(
        page: Int,
        genres: String?,
        sortBy: String,
        minRating: Double,
        year: Int?
    ): Result<MediaListResponse> =
        categoryToResults(MediaCategory.POPULAR_SERIES, page)

    override suspend fun getWatchlist(): Result<List<Media>> = withContext(Dispatchers.IO) {
        Result.success(emptyList())
    }

    override suspend fun addToWatchlist(media: Media): Result<Unit> = withContext(Dispatchers.IO) {
        Result.success(Unit)
    }

    override suspend fun removeFromWatchlist(mediaId: String): Result<Unit> = withContext(Dispatchers.IO) {
        Result.success(Unit)
    }

    override suspend fun isWatchlisted(mediaId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        Result.success(false)
    }

    override suspend fun getContinueWatching(): Result<List<Media>> = withContext(Dispatchers.IO) {
        Result.success(emptyList())
    }

    override suspend fun updatePlaybackPosition(
        mediaId: String,
        position: Long,
        duration: Long
    ): Result<Unit> = withContext(Dispatchers.IO) {
        Result.success(Unit)
    }

    override suspend fun removeFromContinueWatching(mediaId: String): Result<Unit> = withContext(Dispatchers.IO) {
        Result.success(Unit)
    }

    override suspend fun getDownloads(): Result<List<Media>> = withContext(Dispatchers.IO) {
        Result.success(emptyList())
    }

    override suspend fun addDownload(media: Media, quality: String): Result<Unit> = withContext(Dispatchers.IO) {
        Result.success(Unit)
    }

    override suspend fun removeDownload(mediaId: String): Result<Unit> = withContext(Dispatchers.IO) {
        Result.success(Unit)
    }

    override suspend fun updateDownloadStatus(
        mediaId: String,
        status: DownloadStatus,
        progress: Float
    ): Result<Unit> = withContext(Dispatchers.IO) {
        Result.success(Unit)
    }

    override suspend fun clearCache(): Result<Unit> = withContext(Dispatchers.IO) {
        Result.success(Unit)
    }

    override suspend fun getCacheSize(): Result<Long> = withContext(Dispatchers.IO) {
        Result.success(0L)
    }
}
