package com.nonton.app.data.repository

import com.nonton.app.data.model.*
import kotlinx.coroutines.flow.Flow

interface MediaRepository {

    // Home
    suspend fun getHomePage(): Result<HomePageResponse>

    // Movies
    suspend fun getPopularMovies(page: Int): Result<MediaListResponse>
    suspend fun getTopRatedMovies(page: Int): Result<MediaListResponse>
    suspend fun getNowPlayingMovies(page: Int): Result<MediaListResponse>
    suspend fun getMovieDetail(movieId: String): Result<Media>

    /** Resolve a playable video source (URL and optional request headers) from the active provider. */
    suspend fun resolveVideoSource(movieId: String): Result<VideoSource>
    suspend fun getSimilarMovies(movieId: String, page: Int): Result<MediaListResponse>
    suspend fun getMovieRecommendations(movieId: String, page: Int): Result<MediaListResponse>
    suspend fun getMovieCredits(movieId: String): Result<CreditsResponse>
    suspend fun getMovieProviders(movieId: String): Result<ProviderResponse>

    // TV Series
    suspend fun getPopularSeries(page: Int): Result<MediaListResponse>
    suspend fun getTopRatedSeries(page: Int): Result<MediaListResponse>
    suspend fun getOnTheAirSeries(page: Int): Result<MediaListResponse>
    suspend fun getSeriesDetail(seriesId: String): Result<Media>
    suspend fun getSeasonDetail(seriesId: String, seasonNumber: Int): Result<Season>
    suspend fun getSimilarSeries(seriesId: String, page: Int): Result<MediaListResponse>
    suspend fun getSeriesRecommendations(seriesId: String, page: Int): Result<MediaListResponse>
    suspend fun getSeriesCredits(seriesId: String): Result<CreditsResponse>
    suspend fun getSeriesProviders(seriesId: String): Result<ProviderResponse>

    // Anime
    suspend fun getPopularAnime(page: Int): Result<MediaListResponse>
    suspend fun getAnimeDetail(animeId: String): Result<Media>

    // Drakor
    suspend fun getPopularDrakor(page: Int): Result<MediaListResponse>
    suspend fun getDrakorDetail(drakorId: String): Result<Media>

    // Search
    suspend fun searchMulti(query: String, page: Int): Result<SearchResponse>
    suspend fun searchMovies(query: String, page: Int): Result<MediaListResponse>
    suspend fun searchSeries(query: String, page: Int): Result<MediaListResponse>
    suspend fun searchAnime(query: String, page: Int): Result<MediaListResponse>

    // Genres
    suspend fun getMovieGenres(): Result<GenreResponse>
    suspend fun getTvGenres(): Result<GenreResponse>

    // Trending
    suspend fun getTrending(timeWindow: String): Result<MediaListResponse>

    // Discover
    suspend fun discoverMovies(
        page: Int,
        genres: String?,
        sortBy: String,
        minRating: Double,
        year: Int?
    ): Result<MediaListResponse>

    suspend fun discoverSeries(
        page: Int,
        genres: String?,
        sortBy: String,
        minRating: Double,
        year: Int?
    ): Result<MediaListResponse>

    // Watchlist
    suspend fun getWatchlist(): Result<List<Media>>
    suspend fun addToWatchlist(media: Media): Result<Unit>
    suspend fun removeFromWatchlist(mediaId: String): Result<Unit>
    suspend fun isWatchlisted(mediaId: String): Result<Boolean>

    // Continue Watching
    suspend fun getContinueWatching(): Result<List<Media>>
    suspend fun updatePlaybackPosition(mediaId: String, position: Long, duration: Long): Result<Unit>
    suspend fun removeFromContinueWatching(mediaId: String): Result<Unit>

    // Downloads
    suspend fun getDownloads(): Result<List<Media>>
    suspend fun addDownload(media: Media, quality: String): Result<Unit>
    suspend fun removeDownload(mediaId: String): Result<Unit>
    suspend fun updateDownloadStatus(mediaId: String, status: DownloadStatus, progress: Float): Result<Unit>

    // Cache
    suspend fun clearCache(): Result<Unit>
    suspend fun getCacheSize(): Result<Long>
}