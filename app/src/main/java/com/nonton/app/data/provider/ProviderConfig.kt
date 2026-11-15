package com.nonton.app.data.provider

import kotlinx.serialization.Serializable

/**
 * Konfigurasi provider yang diunduh dari server remote atau dibundle dalam assets.
 * Setiap config berisi aturan scraping untuk satu situs streaming.
 */
@Serializable
data class ProviderConfig(
    val id: String,
    val name: String,
    val baseUrl: String,
    val description: String = "",
    val logoUrl: String? = null,
    /** URL of a JSON provider configuration that can be refreshed manually. */
    val downloadUrl: String? = null,
    val isEnabled: Boolean = true,
    val priority: Int = 0,
    val version: Int = 1,
    val type: ProviderType = ProviderType.MOVIE,
    /** Format returned by the provider's list, search, and detail endpoints. */
    val responseType: ProviderResponseType = ProviderResponseType.HTML,
    val selectors: ProviderSelectors = ProviderSelectors(),
    val endpoints: ProviderEndpoints = ProviderEndpoints(),
    /** Optional field mapping used when [responseType] is [ProviderResponseType.JSON]. */
    val json: ProviderJsonSelectors? = null,
    val headers: Map<String, String> = emptyMap()
) {
    /** True when this configuration can be used as the active content provider. */
    val isUsable: Boolean
        get() = isEnabled && baseUrl.isNotBlank()
}

@Serializable
enum class ProviderType {
    MOVIE,
    SERIES,
    ANIME,
    DRAKOR,
    MIXED
}

/** Response format consumed by the provider. */
@Serializable
enum class ProviderResponseType {
    HTML,
    JSON
}

/**
 * Dot-separated JSON paths used by JSON providers.
 *
 * Paths are evaluated against the complete response. For example,
 * `response.docs` selects the list from an Internet Archive search response,
 * while `metadata` selects the detail object from its metadata response.
 */
@Serializable
data class ProviderJsonSelectors(
    /** Path to the array containing list/search items. */
    val listPath: String = "data",
    val idPath: String = "id",
    val titlePath: String = "title",
    val posterPath: String? = null,
    /** URL template for a poster. Supports `{id}`. */
    val posterUrlTemplate: String? = null,
    val overviewPath: String? = null,
    val yearPath: String? = null,
    val ratingPath: String? = null,
    /** Path to the object containing detail fields. Empty means the root object. */
    val detailPath: String = "",
    val detailTitlePath: String = "title",
    val detailPosterPath: String? = null,
    /** URL template for a detail poster. Supports `{id}`. */
    val detailPosterUrlTemplate: String? = null,
    val detailOverviewPath: String? = null,
    val detailYearPath: String? = null,
    val detailRatingPath: String? = null,
    /** Path to an array of playable files in the detail response. */
    val videoFilesPath: String? = null,
    val videoFileNamePath: String = "name",
    val videoFileFormatPath: String? = null,
    val videoFileSourcePath: String? = null,
    /** Optional required file source, for example `derivative`. */
    val videoFileSource: String? = null,
    /** Preferred formats, ordered from best to worst for mobile playback. */
    val videoFileFormatPreference: List<String> = emptyList(),
    /** Optional required file format, for example `MPEG4`. */
    val videoFileFormat: String? = null,
    /** Optional required file extension, for example `mp4`. */
    val videoFileExtension: String? = null,
    /** URL template for a selected file. Supports `{id}` and `{filename}`. */
    val videoFileUrlTemplate: String? = null
)

@Serializable
data class ProviderSelectors(
    val movieItem: String = ".movie-item, .film-card, .post-item",
    val title: String = ".title, h2, h3",
    val detailLink: String = "a",
    val poster: String = "img",
    val rating: String = ".rating, .score, .star",
    val year: String = ".year, .tahun",
    val detailTitle: String = "h1, .title, .judul",
    val detailOverview: String = ".synopsis, .deskripsi, .overview",
    val detailRating: String = ".rating, .score",
    val detailPoster: String = "img.poster, img.cover",
    val detailYear: String = ".year, .tahun",
    /** Elements that expose a direct media URL. */
    val videoSource: String = "video source[src], video[src]",
    /** Links that lead to a watch page. */
    val videoLink: String = "a[href*='play'], a[href*='watch'], a[href*='video']",
    /** Embedded players that may expose the final media URL. */
    val videoIframe: String = "iframe[src]"
)

@Serializable
data class ProviderEndpoints(
    val popularMovies: String = "/film-populer",
    val popularSeries: String = "/series-populer",
    val popularDrakor: String = "/drakor",
    val popularAnime: String = "/anime",
    val search: String = "/cari?q={query}",
    val detail: String = "/detail/{id}",
    /** Optional watch-page/API endpoint. The {id} placeholder is supported. */
    val videoEndpoint: String? = null,
    /** Optional direct media URL template. The {id} placeholder is supported. */
    val videoUrlTemplate: String? = null
)
