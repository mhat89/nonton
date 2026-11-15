package com.nonton.app.data.model

import com.squareup.moshi.Json
import kotlinx.serialization.Serializable

@Serializable
data class Media(
    val id: String,
    val title: String,
    @Json(name = "original_title") val originalTitle: String? = null,
    val overview: String? = null,
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "backdrop_path") val backdropPath: String? = null,
    @Json(name = "release_date") val releaseDate: String? = null,
    @Json(name = "first_air_date") val firstAirDate: String? = null,
    @Json(name = "vote_average") val voteAverage: Double = 0.0,
    @Json(name = "vote_count") val voteCount: Int = 0,
    val popularity: Double = 0.0,
    @Json(name = "genre_ids") val genreIds: List<Int> = emptyList(),
    val genres: List<Genre> = emptyList(),
    @Json(name = "media_type") val mediaType: MediaType = MediaType.MOVIE,
    val status: String? = null,
    val runtime: Int? = null,
    @Json(name = "number_of_seasons") val numberOfSeasons: Int? = null,
    @Json(name = "number_of_episodes") val numberOfEpisodes: Int? = null,
    val seasons: List<Season> = emptyList(),
    val videos: VideoResponse? = null,
    val images: ImagesResponse? = null,
    @Json(name = "external_ids") val externalIds: ExternalIds? = null,
    val providers: Map<String, List<Provider>> = emptyMap(),
    @Json(name = "is_watchlisted") val isWatchlisted: Boolean = false,
    @Json(name = "playback_position") val playbackPosition: Long = 0,
    @Json(name = "total_duration") val totalDuration: Long = 0,
    @Json(name = "last_watched_at") val lastWatchedAt: String? = null,
    @Json(name = "download_status") val downloadStatus: DownloadStatus = DownloadStatus.NOT_DOWNLOADED,
    @Json(name = "download_path") val downloadPath: String? = null,
    @Json(name = "downloaded_quality") val downloadedQuality: String? = null
) {
    val year: String
        get() = releaseDate?.take(4) ?: firstAirDate?.take(4) ?: "N/A"

    val formattedRating: String
        get() = String.format("%.1f", voteAverage)

    val isMovie: Boolean
        get() = mediaType == MediaType.MOVIE

    val isSeries: Boolean
        get() = mediaType == MediaType.TV_SERIES

    val isAnime: Boolean
        get() = mediaType == MediaType.ANIME

    val isDrakor: Boolean
        get() = mediaType == MediaType.DRAKOR
}

@Serializable
enum class MediaType {
    @Json(name = "movie")
    MOVIE,

    @Json(name = "tv")
    TV_SERIES,

    @Json(name = "anime")
    ANIME,

    @Json(name = "drakor")
    DRAKOR,

    @Json(name = "live_tv")
    LIVE_TV
}

/** Categories used by the home and browse screens. */
enum class MediaCategory {
    POPULAR,
    TOP_RATED,
    NOW_PLAYING,
    POPULAR_SERIES,
    TOP_RATED_SERIES,
    ON_THE_AIR,
    POPULAR_ANIME,
    POPULAR_DRAKOR,
    TRENDING_DAY,
    TRENDING_WEEK
}

@Serializable
data class Genre(
    val id: Int,
    val name: String
)

@Serializable
data class Season(
    val id: String,
    @Json(name = "season_number") val seasonNumber: Int,
    val name: String,
    val overview: String? = null,
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "air_date") val airDate: String? = null,
    @Json(name = "episode_count") val episodeCount: Int = 0,
    val episodes: List<Episode> = emptyList()
)

@Serializable
data class Episode(
    val id: String,
    @Json(name = "season_number") val seasonNumber: Int,
    @Json(name = "episode_number") val episodeNumber: Int,
    val name: String,
    val overview: String? = null,
    @Json(name = "still_path") val stillPath: String? = null,
    @Json(name = "air_date") val airDate: String? = null,
    val runtime: Int? = null,
    @Json(name = "vote_average") val voteAverage: Double = 0.0,
    @Json(name = "vote_count") val voteCount: Int = 0,
    @Json(name = "video_url") val videoUrl: String? = null,
    @Json(name = "download_status") val downloadStatus: DownloadStatus = DownloadStatus.NOT_DOWNLOADED,
    @Json(name = "download_path") val downloadPath: String? = null,
    @Json(name = "playback_position") val playbackPosition: Long = 0,
    @Json(name = "total_duration") val totalDuration: Long = 0
)

@Serializable
data class VideoResponse(
    val results: List<Video> = emptyList()
)

/** A playable media URL together with the headers required by the provider. */
@Serializable
data class VideoSource(
    val url: String,
    val headers: Map<String, String> = emptyMap()
)

@Serializable
data class Video(
    val id: String,
    val key: String,
    val name: String,
    val site: String,
    val type: String,
    val official: Boolean = false
)

@Serializable
data class ImagesResponse(
    val backdrops: List<Image> = emptyList(),
    val posters: List<Image> = emptyList(),
    val logos: List<Image> = emptyList()
)

@Serializable
data class Image(
    @Json(name = "file_path") val filePath: String,
    val width: Int,
    val height: Int,
    @Json(name = "aspect_ratio") val aspectRatio: Double,
    @Json(name = "vote_average") val voteAverage: Double,
    @Json(name = "vote_count") val voteCount: Int,
    @Json(name = "iso_639_1") val iso6391: String? = null
)

@Serializable
data class ExternalIds(
    @Json(name = "imdb_id") val imdbId: String? = null,
    @Json(name = "tmdb_id") val tmdbId: Int? = null,
    @Json(name = "tvdb_id") val tvdbId: Int? = null,
    @Json(name = "facebook_id") val facebookId: String? = null,
    @Json(name = "instagram_id") val instagramId: String? = null,
    @Json(name = "twitter_id") val twitterId: String? = null
)

@Serializable
data class Provider(
    @Json(name = "provider_id") val providerId: Int,
    @Json(name = "provider_name") val providerName: String,
    @Json(name = "logo_path") val logoPath: String? = null,
    @Json(name = "display_priority") val displayPriority: Int = 0,
    val link: String? = null
)

@Serializable
enum class DownloadStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    DOWNLOADED,
    FAILED,
    PAUSED
}

@Serializable
data class MediaListResponse(
    val page: Int = 1,
    val results: List<Media> = emptyList(),
    @Json(name = "total_pages") val totalPages: Int = 1,
    @Json(name = "total_results") val totalResults: Int = 0
)

@Serializable
data class SearchResponse(
    val page: Int = 1,
    val results: List<Media> = emptyList(),
    @Json(name = "total_pages") val totalPages: Int = 1,
    @Json(name = "total_results") val totalResults: Int = 0,
    val query: String = ""
)

@Serializable
data class GenreResponse(
    val genres: List<Genre> = emptyList()
)

@Serializable
data class ProviderResponse(
    val results: Map<String, List<Provider>> = emptyMap()
)

@Serializable
data class HomePageResponse(
    @Json(name = "continue_watching") val continueWatching: List<Media> = emptyList(),
    @Json(name = "trending_movies") val trendingMovies: List<Media> = emptyList(),
    @Json(name = "trending_series") val trendingSeries: List<Media> = emptyList(),
    @Json(name = "popular_drakor") val popularDrakor: List<Media> = emptyList(),
    @Json(name = "popular_anime") val popularAnime: List<Media> = emptyList(),
    @Json(name = "top_rated") val topRated: List<Media> = emptyList(),
    @Json(name = "new_releases") val newReleases: List<Media> = emptyList(),
    @Json(name = "live_channels") val liveChannels: List<Media> = emptyList(),
    val genres: Map<String, List<Media>> = emptyMap()
)

@Serializable
data class CreditsResponse(
    val id: Int,
    val cast: List<CastMember> = emptyList(),
    val crew: List<CrewMember> = emptyList()
)

@Serializable
data class CastMember(
    val id: Int,
    val name: String,
    val character: String,
    @Json(name = "profile_path") val profilePath: String? = null,
    val order: Int = 0
)

@Serializable
data class CrewMember(
    val id: Int,
    val name: String,
    val job: String,
    val department: String,
    @Json(name = "profile_path") val profilePath: String? = null
)
