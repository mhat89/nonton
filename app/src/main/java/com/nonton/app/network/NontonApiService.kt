package com.nonton.app.network

import com.nonton.app.data.model.*
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface NontonApiService {

    // Home Page
    @GET("home")
    suspend fun getHomePage(): Response<HomePageResponse>

    // Movies
    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Query("page") page: Int = 1,
        @Query("language") language: String = "id-ID"
    ): Response<MediaListResponse>

    @GET("movie/top_rated")
    suspend fun getTopRatedMovies(
        @Query("page") page: Int = 1,
        @Query("language") language: String = "id-ID"
    ): Response<MediaListResponse>

    @GET("movie/now_playing")
    suspend fun getNowPlayingMovies(
        @Query("page") page: Int = 1,
        @Query("language") language: String = "id-ID"
    ): Response<MediaListResponse>

    @GET("movie/{movie_id}")
    suspend fun getMovieDetail(
        @Path("movie_id") movieId: String,
        @Query("append_to_response") appendToResponse: String = "videos,images,external_ids,credits,providers",
        @Query("language") language: String = "id-ID"
    ): Response<Media>

    // TV Series
    @GET("tv/popular")
    suspend fun getPopularSeries(
        @Query("page") page: Int = 1,
        @Query("language") language: String = "id-ID"
    ): Response<MediaListResponse>

    @GET("tv/top_rated")
    suspend fun getTopRatedSeries(
        @Query("page") page: Int = 1,
        @Query("language") language: String = "id-ID"
    ): Response<MediaListResponse>

    @GET("tv/on_the_air")
    suspend fun getOnTheAirSeries(
        @Query("page") page: Int = 1,
        @Query("language") language: String = "id-ID"
    ): Response<MediaListResponse>

    @GET("tv/{tv_id}")
    suspend fun getSeriesDetail(
        @Path("tv_id") seriesId: String,
        @Query("append_to_response") appendToResponse: String = "videos,images,external_ids,credits,providers,season/1",
        @Query("language") language: String = "id-ID"
    ): Response<Media>

    @GET("tv/{tv_id}/season/{season_number}")
    suspend fun getSeasonDetail(
        @Path("tv_id") seriesId: String,
        @Path("season_number") seasonNumber: Int,
        @Query("language") language: String = "id-ID"
    ): Response<Season>

    // Anime
    @GET("anime/popular")
    suspend fun getPopularAnime(
        @Query("page") page: Int = 1,
        @Query("language") language: String = "id-ID"
    ): Response<MediaListResponse>

    @GET("anime/{anime_id}")
    suspend fun getAnimeDetail(
        @Path("anime_id") animeId: String,
        @Query("append_to_response") appendToResponse: String = "videos,images,external_ids,providers",
        @Query("language") language: String = "id-ID"
    ): Response<Media>

    // Drakor
    @GET("drakor/popular")
    suspend fun getPopularDrakor(
        @Query("page") page: Int = 1,
        @Query("language") language: String = "id-ID"
    ): Response<MediaListResponse>

    @GET("drakor/{drakor_id}")
    suspend fun getDrakorDetail(
        @Path("drakor_id") drakorId: String,
        @Query("append_to_response") appendToResponse: String = "videos,images,external_ids,providers",
        @Query("language") language: String = "id-ID"
    ): Response<Media>

    // Search
    @GET("search/multi")
    suspend fun searchMulti(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "id-ID",
        @Query("include_adult") includeAdult: Boolean = false
    ): Response<SearchResponse>

    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "id-ID"
    ): Response<MediaListResponse>

    @GET("search/tv")
    suspend fun searchSeries(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "id-ID"
    ): Response<MediaListResponse>

    @GET("search/anime")
    suspend fun searchAnime(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "id-ID"
    ): Response<MediaListResponse>

    // Genres
    @GET("genre/movie/list")
    suspend fun getMovieGenres(
        @Query("language") language: String = "id-ID"
    ): Response<GenreResponse>

    @GET("genre/tv/list")
    suspend fun getTvGenres(
        @Query("language") language: String = "id-ID"
    ): Response<GenreResponse>

    // Watch Providers
    @GET("movie/{movie_id}/watch/providers")
    suspend fun getMovieProviders(@Path("movie_id") movieId: String): Response<ProviderResponse>

    @GET("tv/{tv_id}/watch/providers")
    suspend fun getSeriesProviders(@Path("tv_id") seriesId: String): Response<ProviderResponse>

    // Discover
    @GET("discover/movie")
    suspend fun discoverMovies(
        @Query("page") page: Int = 1,
        @Query("with_genres") genres: String? = null,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("vote_average.gte") minRating: Double = 0.0,
        @Query("year") year: Int? = null,
        @Query("language") language: String = "id-ID"
    ): Response<MediaListResponse>

    @GET("discover/tv")
    suspend fun discoverSeries(
        @Query("page") page: Int = 1,
        @Query("with_genres") genres: String? = null,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("vote_average.gte") minRating: Double = 0.0,
        @Query("first_air_date_year") year: Int? = null,
        @Query("language") language: String = "id-ID"
    ): Response<MediaListResponse>

    // Trending
    @GET("trending/all/{time_window}")
    suspend fun getTrending(
        @Path("time_window") timeWindow: String = "day",
        @Query("language") language: String = "id-ID"
    ): Response<MediaListResponse>

    // Similar
    @GET("movie/{movie_id}/similar")
    suspend fun getSimilarMovies(
        @Path("movie_id") movieId: String,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "id-ID"
    ): Response<MediaListResponse>

    @GET("tv/{tv_id}/similar")
    suspend fun getSimilarSeries(
        @Path("tv_id") seriesId: String,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "id-ID"
    ): Response<MediaListResponse>

    // Recommendations
    @GET("movie/{movie_id}/recommendations")
    suspend fun getMovieRecommendations(
        @Path("movie_id") movieId: String,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "id-ID"
    ): Response<MediaListResponse>

    @GET("tv/{tv_id}/recommendations")
    suspend fun getSeriesRecommendations(
        @Path("tv_id") seriesId: String,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "id-ID"
    ): Response<MediaListResponse>

    // Credits
    @GET("movie/{movie_id}/credits")
    suspend fun getMovieCredits(@Path("movie_id") movieId: String): Response<CreditsResponse>

    @GET("tv/{tv_id}/credits")
    suspend fun getSeriesCredits(@Path("tv_id") seriesId: String): Response<CreditsResponse>
}
