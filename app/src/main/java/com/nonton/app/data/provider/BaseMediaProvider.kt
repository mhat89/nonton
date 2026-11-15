package com.nonton.app.data.provider

import android.util.Log
import com.nonton.app.data.model.Media
import com.nonton.app.data.model.MediaCategory
import com.nonton.app.data.model.MediaListResponse
import com.nonton.app.data.model.MediaType
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

/**
 * Kelas dasar untuk semua provider. Menyediakan OkHttp client dan helper
 * untuk melakukan request serta parsing HTML dengan Jsoup.
 */
abstract class BaseMediaProvider : MediaProvider {

    protected val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    protected companion object {
        private const val TAG = "BaseMediaProvider"
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }

    /**
     * Lakukan GET request dan kembalikan Document Jsoup.
     */
    suspend fun fetchDocument(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): Document? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml")
                .header("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8")
                .apply {
                    headers.forEach { (name, value) -> header(name, value) }
                }
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "HTTP ${response.code} saat fetch $url")
                    return@withContext null
                }
                val html = response.body?.string() ?: return@withContext null
                Jsoup.parse(html)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal fetch $url", e)
            null
        }
    }

    /**
     * Lakukan GET request dan kembalikan teks mentah.
     */
    suspend fun fetchText(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/json,*/*")
                .apply {
                    headers.forEach { (name, value) -> header(name, value) }
                }
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "HTTP ${response.code} saat fetch text $url")
                    return@withContext null
                }
                response.body?.string()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal fetch text $url", e)
            null
        }
    }

    /**
     * Enkode kueri pencarian untuk URL.
     */
    protected fun encodeQuery(query: String): String =
        URLEncoder.encode(query, StandardCharsets.UTF_8.toString())

    /**
     * Ekstrak angka dari string (contoh: "8.5" -> 8.5).
     */
    protected fun parseRating(text: String?): Double {
        if (text.isNullOrBlank()) return 0.0
        val digits = text.filter { it.isDigit() || it == '.' }
        return digits.toDoubleOrNull() ?: 0.0
    }

    /**
     * Ekstrak tahun dari string.
     */
    protected fun parseYear(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val regex = java.util.regex.Pattern.compile("\\b(19|20)\\d{2}\\b")
        val matcher = regex.matcher(text)
        return if (matcher.find()) matcher.group() else null
    }

    protected open fun createMedia(
        id: String,
        title: String,
        year: String?,
        rating: Double,
        posterPath: String?,
        mediaType: MediaType,
        overview: String? = null
    ): Media = Media(
        id = id,
        title = title,
        overview = overview,
        releaseDate = year?.let { "$it-01-01" },
        voteAverage = rating,
        posterPath = posterPath,
        mediaType = mediaType
    )

    override suspend fun getMediaDetail(mediaId: String): Media? = null

    override suspend fun getVideoUrl(mediaId: String): String? = null

    override suspend fun isAvailable(): Boolean = true

    override suspend fun searchMedia(query: String, page: Int): MediaListResponse {
        return MediaListResponse(page = page, results = emptyList())
    }

    abstract override suspend fun getMediaList(
        category: MediaCategory,
        page: Int
    ): MediaListResponse
}
