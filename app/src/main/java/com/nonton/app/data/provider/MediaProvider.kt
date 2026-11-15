package com.nonton.app.data.provider

import com.nonton.app.data.model.Media
import com.nonton.app.data.model.MediaCategory
import com.nonton.app.data.model.MediaListResponse
import com.nonton.app.data.model.MediaType
import kotlinx.coroutines.flow.Flow

/**
 * Antarmuka untuk semua provider konten (scraper).
 * Setiap provider bertanggung jawab untuk mengambil data dari satu sumber publik
 * (mirip dengan sistem provider/ekstensi di AdiXtream / CloudStream).
 */
interface MediaProvider {

    /** Nama unik provider (contoh: "AdiDrakor", "Idlix", "Adicinemax21") */
    val providerName: String

    /** ID unik provider (contoh: "adidrakor", "idlix", "adicinemax21") */
    val providerId: String

    /**
     * Mengambil daftar konten berdasarkan kategori.
     * Jika provider tidak mendukung kategori, kembalikan daftar kosong.
     */
    suspend fun getMediaList(
        category: MediaCategory,
        page: Int
    ): MediaListResponse

    /**
     * Mencari konten berdasarkan kueri.
     */
    suspend fun searchMedia(
        query: String,
        page: Int
    ): MediaListResponse

    /**
     * Mengambil detail konten berdasarkan ID.
     */
    suspend fun getMediaDetail(mediaId: String): Media?

    /**
     * Mendapatkan URL video untuk pemutaran.
     */
    suspend fun getVideoUrl(mediaId: String): String?

    /** Headers that must be sent when the resolved video URL is requested. */
    suspend fun getVideoHeaders(): Map<String, String> = emptyMap()

    /**
     * Mengecek apakah provider saat ini tersedia (online).
     */
    suspend fun isAvailable(): Boolean
}

/**
 * Hasil pencarian dari provider.
 */
data class ProviderSearchResult(
    val results: List<Media> = emptyList(),
    val totalPages: Int = 1,
    val totalResults: Int = 0,
    val query: String = ""
)

/**
 * Aliran (Flow) untuk pembaruan real-time dari provider (opsional).
 */
interface MediaProviderFlow {
    fun getMediaListFlow(category: MediaCategory, page: Int): Flow<MediaListResponse>
}
