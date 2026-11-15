package com.nonton.app.data.provider

import com.nonton.app.data.model.Media
import com.nonton.app.data.model.MediaCategory
import com.nonton.app.data.model.MediaListResponse
import com.nonton.app.data.model.MediaType
import okhttp3.OkHttpClient
import org.jsoup.nodes.Element

/**
 * Contoh template provider scraper untuk situs streaming publik.
 *
 * Untuk mengaktifkan provider nyata:
 * 1. Ganti BASE_URL dengan situs target.
 * 2. Sesuaikan CSS selector sesuai struktur HTML situs tersebut.
 * 3. Implementasikan getVideoUrl() untuk mengekstrak URL video.
 * 4. Daftarkan provider di ProviderModule.
 *
 * Contoh provider ala AdiXtream: AdiDrakor, Idlix, Adicinemax21, dll.
 */
class ExampleScraperProvider : BaseMediaProvider() {

    override val providerName: String = "Example Scraper"
    override val providerId: String = "example_scraper"

    // Ganti dengan URL situs target yang ingin di-scrape
    private val baseUrl = "https://contoh-situs-streaming.com"

    override suspend fun getMediaList(
        category: MediaCategory,
        page: Int
    ): MediaListResponse {
        val url = buildListUrl(category, page)
        val doc = fetchDocument(url) ?: return MediaListResponse(page = page)

        val results = mutableListOf<Media>()
        // SESUAIKAN selector dengan struktur HTML situs target
        val items = doc.select(".movie-item, .film-card, .post-item")

        for (element in items) {
            val media = parseMediaElement(element)
            if (media != null) {
                results.add(media)
            }
        }

        return MediaListResponse(
            page = page,
            results = results,
            totalPages = 1,
            totalResults = results.size
        )
    }

    private fun buildListUrl(category: MediaCategory, page: Int): String {
        val path = when (category) {
            MediaCategory.POPULAR -> "/film-populer"
            MediaCategory.POPULAR_SERIES -> "/series-populer"
            MediaCategory.POPULAR_DRAKOR -> "/drakor"
            MediaCategory.POPULAR_ANIME -> "/anime"
            else -> "/film"
        }
        return "$baseUrl$path?page=$page"
    }

    private fun parseMediaElement(element: Element): Media? {
        try {
            val title = element.select(".title, h2, h3").first()?.text() ?: return null
            val detailUrl = element.select("a").first()?.attr("href") ?: return null
            val id = detailUrl.substringAfterLast("/").takeIf { it.isNotBlank() } ?: detailUrl
            val poster = element.select("img").first()?.attr("src")
            val ratingText = element.select(".rating, .score, .star").first()?.text()
            val rating = parseRating(ratingText)
            val yearText = element.select(".year, .tahun").first()?.text()
            val year = parseYear(yearText)

            return createMedia(
                id = id,
                title = title,
                year = year,
                rating = rating,
                posterPath = poster,
                mediaType = MediaType.MOVIE, // Sesuaikan dengan tipe konten
                overview = null
            )
        } catch (e: Exception) {
            return null
        }
    }

    override suspend fun searchMedia(query: String, page: Int): MediaListResponse {
        val searchUrl = "$baseUrl/cari?q=${encodeQuery(query)}&page=$page"
        val doc = fetchDocument(searchUrl) ?: return MediaListResponse(page = page)

        val results = mutableListOf<Media>()
        val items = doc.select(".movie-item, .film-card")

        for (element in items) {
            val media = parseMediaElement(element)
            if (media != null) {
                results.add(media)
            }
        }

        return MediaListResponse(
            page = page,
            results = results,
            totalPages = 1,
            totalResults = results.size
        )
    }

    override suspend fun getMediaDetail(mediaId: String): Media? {
        val detailUrl = "$baseUrl/detail/$mediaId"
        val doc = fetchDocument(detailUrl) ?: return null

        val title = doc.select("h1, .title, .judul").first()?.text() ?: return null
        val overview = doc.select(".synopsis, .deskripsi, .overview").first()?.text()
        val ratingText = doc.select(".rating, .score").first()?.text()
        val rating = parseRating(ratingText)
        val poster = doc.select("img.poster, img.cover").first()?.attr("src")
        val yearText = doc.select(".year, .tahun").first()?.text()
        val year = parseYear(yearText)

        return createMedia(
            id = mediaId,
            title = title,
            year = year,
            rating = rating,
            posterPath = poster,
            mediaType = MediaType.MOVIE,
            overview = overview
        )
    }

    override suspend fun getVideoUrl(mediaId: String): String? {
        // Ekstrak URL video dari halaman detail.
        // Setiap situs memiliki cara berbeda untuk menyembunyikan URL video.
        return null
    }
}
