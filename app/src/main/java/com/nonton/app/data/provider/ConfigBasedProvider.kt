package com.nonton.app.data.provider

import com.nonton.app.data.model.Media
import com.nonton.app.data.model.MediaCategory
import com.nonton.app.data.model.MediaListResponse
import com.nonton.app.data.model.MediaType
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser

/**
 * A provider whose selectors and endpoints are supplied by a JSON configuration.
 *
 * The scraper only reads pages that the user explicitly configures. It does
 * not contain bundled mock content or a private application backend.
 */
class ConfigBasedProvider : BaseMediaProvider() {

    private val json = Json { ignoreUnknownKeys = true }

    private var config: ProviderConfig = ProviderConfig(
        id = "unconfigured",
        name = "Belum ada provider",
        baseUrl = "",
        isEnabled = false
    )

    override val providerName: String
        get() = config.name

    override val providerId: String
        get() = config.id

    fun setConfig(newConfig: ProviderConfig) {
        config = newConfig
    }

    override suspend fun getMediaList(
        category: MediaCategory,
        page: Int
    ): MediaListResponse {
        requireConfigured()

        val endpoint = endpointFor(category)
        if (endpoint.isBlank()) {
            return MediaListResponse(page = page, totalPages = 0, totalResults = 0)
        }

        if (config.responseType == ProviderResponseType.JSON) {
            return getJsonMediaList(endpoint, category, page)
        }

        val url = buildUrl(endpoint, page)
        val doc = fetchDocument(url, config.headers)
            ?: throw IllegalStateException("Gagal membuka halaman provider: $url")

        val results = doc.select(config.selectors.movieItem)
            .mapNotNull { element -> parseMediaElement(element, category) }

        return MediaListResponse(
            page = page,
            results = results,
            totalPages = 1,
            totalResults = results.size
        )
    }

    override suspend fun searchMedia(query: String, page: Int): MediaListResponse {
        requireConfigured()

        val endpoint = config.endpoints.search
        if (endpoint.isBlank()) {
            return MediaListResponse(page = page, totalPages = 0, totalResults = 0)
        }

        val url = buildUrl(endpoint.replace("{query}", encodeQuery(query)), page)
        if (config.responseType == ProviderResponseType.JSON) {
            return getJsonMediaList(url, MediaCategory.POPULAR, page)
        }

        val doc = fetchDocument(url, config.headers)
            ?: throw IllegalStateException("Gagal membuka halaman pencarian provider: $url")

        val results = doc.select(config.selectors.movieItem)
            .mapNotNull { element -> parseMediaElement(element, MediaCategory.POPULAR) }

        return MediaListResponse(
            page = page,
            results = results,
            totalPages = 1,
            totalResults = results.size
        )
    }

    override suspend fun getMediaDetail(mediaId: String): Media? {
        requireConfigured()
        if (mediaId.isBlank()) return null

        val endpoint = config.endpoints.detail.replace("{id}", encodePathSegment(mediaId))
        val url = buildUrl(endpoint)
        if (config.responseType == ProviderResponseType.JSON) {
            return getJsonMediaDetail(mediaId, url)
        }

        val doc = fetchDocument(url, config.headers) ?: return null

        val title = doc.select(config.selectors.detailTitle)
            .firstOrNull { it.text().isNotBlank() }
            ?.text()
            ?.trim()
            ?: mediaId
        val overview = doc.select(config.selectors.detailOverview)
            .firstOrNull { it.text().isNotBlank() }
            ?.text()
            ?.trim()
        val rating = parseRating(
            doc.select(config.selectors.detailRating).firstOrNull()?.text()
        )
        val poster = doc.select(config.selectors.detailPoster)
            .firstOrNull()
            ?.let { element -> imageUrl(element) }
            ?.let { resolveUrl(it, url) }
        val year = parseYear(
            doc.select(config.selectors.detailYear).firstOrNull()?.text()
        )

        return createMedia(
            id = mediaId,
            title = title,
            year = year,
            rating = rating,
            posterPath = poster,
            mediaType = mediaTypeForProvider(),
            overview = overview
        )
    }

    /**
     * Resolves a playable URL from the configured detail/watch page.
     *
     * Supported provider shapes include direct `<video>/<source>` elements,
     * iframe/watch pages, JSON responses, JavaScript player variables, and a
     * direct URL template in the provider configuration.
     */
    override suspend fun getVideoUrl(mediaId: String): String? {
        requireConfigured()
        if (mediaId.isBlank()) return null

        val encodedId = encodePathSegment(mediaId)

        config.endpoints.videoUrlTemplate
            ?.replace("{id}", encodedId)
            ?.takeIf { it.isNotBlank() }
            ?.let { template -> resolveUrl(template, config.baseUrl) }
            ?.takeIf { isHttpUrl(it) }
            ?.let { return it }

        val detailEndpoint = config.endpoints.detail.replace("{id}", encodedId)
        val detailUrl = buildUrl(detailEndpoint)
        if (config.responseType == ProviderResponseType.JSON) {
            return getJsonVideoUrl(mediaId, detailUrl)
        }

        val detailDocument = fetchDocument(detailUrl, config.headers)

        detailDocument?.let { document ->
            extractDirectVideo(document, detailUrl)?.let { return it }
            extractNestedVideo(document, detailUrl)?.let { return it }
        }

        val videoEndpoint = config.endpoints.videoEndpoint
            ?.replace("{id}", encodedId)
            ?.takeIf { it.isNotBlank() }
            ?: return null

        val videoUrl = buildUrl(videoEndpoint)
        val body = fetchText(videoUrl, config.headers) ?: return null

        body.trim().takeIf { isHttpUrl(it) }?.let { return it }
        extractVideoFromJson(body, videoUrl)?.let { return it }
        val videoDocument = Jsoup.parse(body)
        extractDirectVideo(videoDocument, videoUrl)?.let { return it }
        return extractNestedVideo(videoDocument, videoUrl)
    }

    override suspend fun getVideoHeaders(): Map<String, String> = config.headers

    override suspend fun isAvailable(): Boolean =
        config.isEnabled && config.baseUrl.isNotBlank()

    private fun requireConfigured() {
        if (!config.isUsable) {
            throw ProviderNotConfiguredException()
        }
    }

    private fun endpointFor(category: MediaCategory): String = when (category) {
        MediaCategory.POPULAR,
        MediaCategory.TOP_RATED,
        MediaCategory.NOW_PLAYING,
        MediaCategory.TRENDING_DAY,
        MediaCategory.TRENDING_WEEK -> config.endpoints.popularMovies
        MediaCategory.POPULAR_SERIES,
        MediaCategory.TOP_RATED_SERIES,
        MediaCategory.ON_THE_AIR -> config.endpoints.popularSeries
        MediaCategory.POPULAR_ANIME -> config.endpoints.popularAnime
        MediaCategory.POPULAR_DRAKOR -> config.endpoints.popularDrakor
    }

    private fun buildUrl(endpoint: String, page: Int? = null): String {
        var path = endpoint.trim()
        if (page != null) path = path.replace("{page}", page.toString())
        if (path.startsWith("http://") || path.startsWith("https://")) return path
        return "${config.baseUrl.trimEnd('/')}/${path.trimStart('/')}"
    }

    private suspend fun getJsonMediaList(
        endpoint: String,
        category: MediaCategory,
        page: Int
    ): MediaListResponse {
        val mapping = config.json
            ?: throw IllegalStateException("Provider JSON belum memiliki konfigurasi pemetaan respons")
        val url = buildUrl(endpoint, page)
        val body = fetchText(url, config.headers)
            ?: throw IllegalStateException("Gagal membuka endpoint JSON provider: $url")
        val root = parseJsonResponse(body, url)
        val listValue = jsonValueAt(root, mapping.listPath)
        if (listValue == null && mapping.listPath.isNotBlank()) {
            throw IllegalStateException(
                "Path daftar JSON provider tidak ditemukan: ${mapping.listPath}"
            )
        }
        val items = when (listValue) {
            is JsonArray -> listValue
            is JsonObject -> listOf(listValue)
            else -> emptyList()
        }
        val results = items.mapNotNull { item ->
            parseJsonMediaElement(item, mapping, url, category)
        }

        return MediaListResponse(
            page = page,
            results = results,
            totalPages = 1,
            totalResults = results.size
        )
    }

    private suspend fun getJsonMediaDetail(mediaId: String, url: String): Media? {
        val mapping = config.json
            ?: throw IllegalStateException("Provider JSON belum memiliki konfigurasi pemetaan respons")
        val body = fetchText(url, config.headers)
            ?: throw IllegalStateException("Gagal membuka detail JSON provider: $url")
        val root = parseJsonResponse(body, url)
        val detailValue = jsonValueAt(root, mapping.detailPath)
        if (detailValue == null && mapping.detailPath.isNotBlank()) {
            throw IllegalStateException(
                "Path detail JSON provider tidak ditemukan: ${mapping.detailPath}"
            )
        }
        val detailRoot = detailValue ?: root

        val title = jsonString(jsonValueAt(detailRoot, mapping.detailTitlePath))
            ?.let(::plainText)
            ?.takeIf { it.isNotBlank() }
            ?: mediaId
        val overview = jsonString(jsonValueAt(detailRoot, mapping.detailOverviewPath))
            ?.let(::plainText)
            ?.takeIf { it.isNotBlank() }
        val year = parseYear(jsonString(jsonValueAt(detailRoot, mapping.detailYearPath)))
        val rating = parseRating(jsonString(jsonValueAt(detailRoot, mapping.detailRatingPath)))
        val poster = resolveJsonPoster(
            mediaId = mediaId,
            baseUrl = url,
            path = jsonString(jsonValueAt(detailRoot, mapping.detailPosterPath)),
            template = mapping.detailPosterUrlTemplate
        )

        return createMedia(
            id = mediaId,
            title = title,
            year = year,
            rating = rating,
            posterPath = poster,
            mediaType = mediaTypeForProvider(),
            overview = overview
        )
    }

    private suspend fun getJsonVideoUrl(mediaId: String, detailUrl: String): String? {
        val mapping = config.json
            ?: throw IllegalStateException("Provider JSON belum memiliki konfigurasi pemetaan respons")
        val body = fetchText(detailUrl, config.headers)
            ?: throw IllegalStateException("Gagal membuka detail JSON provider: $detailUrl")
        val root = parseJsonResponse(body, detailUrl)

        mapping.videoFilesPath?.let { filesPath ->
            val files = jsonArrayAt(root, filesPath)
            val candidates = files.mapNotNull { file ->
                val filename = jsonString(jsonValueAt(file, mapping.videoFileNamePath))
                    ?.takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null
                val format = jsonString(jsonValueAt(file, mapping.videoFileFormatPath))
                val source = jsonString(jsonValueAt(file, mapping.videoFileSourcePath))
                val extension = mapping.videoFileExtension?.trim()?.trimStart('.')
                if (!extension.isNullOrBlank() &&
                    !filename.substringAfterLast('.', missingDelimiterValue = "").equals(extension, true)
                ) {
                    return@mapNotNull null
                }
                if (!mapping.videoFileFormat.isNullOrBlank() &&
                    !format.equals(mapping.videoFileFormat, ignoreCase = true)
                ) {
                    return@mapNotNull null
                }
                if (!mapping.videoFileSource.isNullOrBlank() &&
                    !source.equals(mapping.videoFileSource, ignoreCase = true)
                ) {
                    return@mapNotNull null
                }

                val formatRank = mapping.videoFileFormatPreference
                    .indexOfFirst { it.equals(format, ignoreCase = true) }
                    .let { if (it < 0) mapping.videoFileFormatPreference.size else it }
                JsonVideoCandidate(
                    filename = filename,
                    formatRank = formatRank
                )
            }.sortedBy { it.formatRank }

            candidates.forEach { candidate ->
                val template = mapping.videoFileUrlTemplate
                    ?.takeIf { it.isNotBlank() }
                    ?: return@forEach
                val url = renderJsonTemplate(template, mediaId, candidate.filename)
                    .let { resolveUrl(it, detailUrl) }
                if (isHttpUrl(url)) return url
            }
        }

        return extractVideoFromJson(body, detailUrl)
    }

    private fun parseJsonMediaElement(
        item: JsonElement,
        mapping: ProviderJsonSelectors,
        baseUrl: String,
        category: MediaCategory
    ): Media? {
        val id = jsonString(jsonValueAt(item, mapping.idPath))
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: return null
        val title = jsonString(jsonValueAt(item, mapping.titlePath))
            ?.let(::plainText)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: return null
        val overview = jsonString(jsonValueAt(item, mapping.overviewPath))
            ?.let(::plainText)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        val year = parseYear(jsonString(jsonValueAt(item, mapping.yearPath)))
        val rating = parseRating(jsonString(jsonValueAt(item, mapping.ratingPath)))
        val poster = resolveJsonPoster(
            mediaId = id,
            baseUrl = baseUrl,
            path = jsonString(jsonValueAt(item, mapping.posterPath)),
            template = mapping.posterUrlTemplate
        )

        return createMedia(
            id = id,
            title = title,
            year = year,
            rating = rating,
            posterPath = poster,
            mediaType = mediaTypeFor(category),
            overview = overview
        )
    }

    private fun resolveJsonPoster(
        mediaId: String,
        baseUrl: String,
        path: String?,
        template: String?
    ): String? {
        val raw = path?.takeIf { it.isNotBlank() }
            ?: template?.let { renderJsonTemplate(it, mediaId, null) }
        return raw?.takeIf { it.isNotBlank() }?.let { resolveUrl(it, baseUrl) }
    }

    private fun parseJsonResponse(body: String, sourceUrl: String): JsonElement =
        try {
            json.parseToJsonElement(body)
        } catch (e: Exception) {
            throw IllegalStateException(
                "Respons JSON provider tidak valid dari $sourceUrl: ${e.message}"
            )
        }

    private fun jsonValueAt(root: JsonElement, path: String?): JsonElement? {
        if (path.isNullOrBlank()) return null
        var current: JsonElement? = root
        path.split('.').filter { it.isNotBlank() }.forEach { segment ->
            current = when (val value = current) {
                is JsonObject -> value[segment]
                is JsonArray -> segment.toIntOrNull()?.let { index -> value.getOrNull(index) }
                else -> null
            }
        }
        return current
    }

    private fun jsonArrayAt(root: JsonElement, path: String): List<JsonElement> =
        when (val value = jsonValueAt(root, path)) {
            is JsonArray -> value
            is JsonObject -> listOf(value)
            else -> emptyList()
        }

    private fun jsonString(value: JsonElement?): String? = when (value) {
        is JsonPrimitive -> if (value is JsonNull) null else value.content.takeIf { it.isNotBlank() }
        is JsonArray -> value.mapNotNull { item -> (item as? JsonPrimitive)?.content }
            .filter { it.isNotBlank() }
            .takeIf { it.isNotEmpty() }
            ?.joinToString(separator = "\n")
        else -> null
    }

    private fun plainText(value: String): String =
        runCatching { Jsoup.parse(value).text() }.getOrDefault(value).trim()

    private fun renderJsonTemplate(template: String, id: String, filename: String?): String =
        template
            .replace("{id}", encodeUrlPath(id))
            .replace(
                "{filename}",
                filename?.let(::encodeUrlPath).orEmpty()
            )

    private fun encodeUrlPath(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
            .replace("+", "%20")
            .replace("%2F", "/")
            .replace("%2f", "/")

    private fun parseMediaElement(element: Element, category: MediaCategory): Media? {
        return try {
            val title = element.select(config.selectors.title)
                .firstOrNull { it.text().isNotBlank() }
                ?.text()
                ?.trim()
                ?: element.selectFirst("a[title]")?.attr("title")?.trim()
                ?: element.selectFirst("img[alt]")?.attr("alt")?.trim()
                ?: return null

            val detailLink = element.select(config.selectors.detailLink)
                .firstOrNull { it.hasAttr("href") }
                ?.let { link -> link.attr("abs:href").ifBlank { link.attr("href") } }
                ?: return null

            val id = mediaIdFromLink(detailLink)
            val poster = element.select(config.selectors.poster)
                .firstOrNull()
                ?.let { imageUrl(it) }
                ?.let { resolveUrl(it, detailLink) }
            val rating = parseRating(
                element.select(config.selectors.rating).firstOrNull()?.text()
            )
            val year = parseYear(
                element.select(config.selectors.year).firstOrNull()?.text()
            )

            createMedia(
                id = id,
                title = title,
                year = year,
                rating = rating,
                posterPath = poster,
                mediaType = mediaTypeFor(category),
                overview = null
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun imageUrl(element: Element): String? =
        sequenceOf("src", "data-src", "data-original", "data-lazy-src", "srcset")
            .map { attribute ->
                element.attr(attribute)
                    .substringBefore(',')
                    .substringBefore(' ')
                    .trim()
            }
            .firstOrNull { it.isNotBlank() }

    private fun mediaIdFromLink(link: String): String {
        val path = link.substringBefore('?').substringBefore('#').trimEnd('/')
        return path.substringAfterLast('/')
            .ifBlank { link }
            .let { runCatching { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }.getOrDefault(it) }
    }

    private fun mediaTypeFor(category: MediaCategory): MediaType = when (category) {
        MediaCategory.POPULAR_SERIES,
        MediaCategory.TOP_RATED_SERIES,
        MediaCategory.ON_THE_AIR -> MediaType.TV_SERIES
        MediaCategory.POPULAR_ANIME -> MediaType.ANIME
        MediaCategory.POPULAR_DRAKOR -> MediaType.DRAKOR
        else -> mediaTypeForProvider()
    }

    private fun mediaTypeForProvider(): MediaType = when (config.type) {
        ProviderType.SERIES -> MediaType.TV_SERIES
        ProviderType.ANIME -> MediaType.ANIME
        ProviderType.DRAKOR -> MediaType.DRAKOR
        else -> MediaType.MOVIE
    }

    private fun extractDirectVideo(document: Document, baseUrl: String): String? {
        val sourceCandidates = buildList {
            document.select(config.selectors.videoSource).forEach { element ->
                add(element.attr("src"))
                add(element.attr("data-src"))
            }
            document.select("video[src], source[src], video source[src]").forEach { element ->
                add(element.attr("src"))
                add(element.attr("data-src"))
            }
        }.mapNotNull { normalizeCandidate(it, baseUrl) }

        val scriptCandidates = document.select("script").flatMap { script ->
            val content = script.data()
            buildList {
                if (script.attr("type").contains("ld+json", ignoreCase = true)) {
                    addAll(jsonLdVideoUrls(content))
                }
                addAll(scriptVideoUrls(content))
            }
        }.mapNotNull { normalizeCandidate(it, baseUrl) }

        val rawCandidates = rawMediaUrls(document.html())
            .mapNotNull { normalizeCandidate(it, baseUrl) }

        return (sourceCandidates + scriptCandidates + rawCandidates)
            .firstOrNull { isDirectMediaUrl(it) }
            ?: scriptDirectVideoUrls(document.html())
                .mapNotNull { normalizeCandidate(it, baseUrl) }
                .firstOrNull { isHttpUrl(it) }
    }

    private suspend fun extractNestedVideo(
        document: Document,
        baseUrl: String,
        depth: Int = 0
    ): String? {
        if (depth >= 2) return null

        val nestedCandidates = buildList {
            document.select(config.selectors.videoLink).forEach { element ->
                add(element.attr("abs:href").ifBlank { element.attr("href") })
            }
            document.select(config.selectors.videoIframe).forEach { element ->
                add(element.attr("abs:src").ifBlank { element.attr("src") })
            }
        }.mapNotNull { normalizeCandidate(it, baseUrl) }
            .filterNot { isDirectMediaUrl(it) }
            .distinct()

        for (candidate in nestedCandidates.take(3)) {
            val nestedDocument = fetchDocument(candidate, config.headers) ?: continue
            extractDirectVideo(nestedDocument, candidate)?.let { return it }
            val deeper = extractNestedVideo(nestedDocument, candidate, depth + 1)
            if (deeper != null) return deeper
        }
        return null
    }

    private fun scriptVideoUrls(script: String): List<String> {
        val patterns = listOf(
            Regex("""(?:file|src|url|playbackUrl|videoUrl|streamUrl)\s*[:=]\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""["'](https?:\\?/\\?/[^"']+\.(?:m3u8|mpd|mp4|webm|mov)(?:\?[^"']*)?)["']""", RegexOption.IGNORE_CASE)
        )
        return patterns.flatMap { pattern -> pattern.findAll(script).map { it.groupValues[1] } }
    }

    private fun scriptDirectVideoUrls(script: String): List<String> = Regex(
        """(?:file|playbackUrl|videoUrl|streamUrl)\s*[:=]\s*["']([^"']+)["']""",
        RegexOption.IGNORE_CASE
    ).findAll(script).map { it.groupValues[1] }.toList()

    private fun rawMediaUrls(html: String): List<String> = Regex(
        """https?:\\?/\\?/[^"'<>\s]+?\.(?:m3u8|mpd|mp4|webm|mov)(?:\?[^"'<>\s]*)?""",
        RegexOption.IGNORE_CASE
    ).findAll(html).map { it.value }.toList()

    private fun jsonLdVideoUrls(content: String): List<String> = try {
        val root = json.parseToJsonElement(content)
        val urls = mutableListOf<String>()
        fun visit(element: kotlinx.serialization.json.JsonElement) {
            when (element) {
                is JsonObject -> {
                    (element["contentUrl"] as? JsonPrimitive)?.content?.let(urls::add)
                    (element["embedUrl"] as? JsonPrimitive)?.content?.let(urls::add)
                    element.values.forEach(::visit)
                }
                is JsonArray -> element.forEach(::visit)
                else -> Unit
            }
        }
        visit(root)
        urls
    } catch (_: Exception) {
        emptyList()
    }

    private fun extractVideoFromJson(body: String, baseUrl: String): String? {
        val root = try {
            json.parseToJsonElement(body)
        } catch (_: Exception) {
            return null
        }

        val allowedKeys = setOf(
            "url",
            "file",
            "src",
            "source",
            "playbackUrl",
            "videoUrl",
            "streamUrl",
            "contentUrl",
            "hls",
            "dash",
            "mp4"
        )
        val candidates = mutableListOf<String>()

        fun visit(element: kotlinx.serialization.json.JsonElement, key: String? = null) {
            when (element) {
                is JsonObject -> element.forEach { (childKey, childValue) ->
                    visit(childValue, childKey)
                }
                is JsonArray -> element.forEach { item -> visit(item, key) }
                is JsonPrimitive -> {
                    if (key in allowedKeys && element.isString) {
                        candidates += element.content
                    }
                }
            }
        }
        visit(root)

        return candidates
            .asSequence()
            .mapNotNull { normalizeCandidate(it, baseUrl) }
            .firstOrNull { isHttpUrl(it) }
    }

    private fun normalizeCandidate(raw: String?, baseUrl: String): String? {
        if (raw.isNullOrBlank()) return null

        var value = Parser.unescapeEntities(raw.trim(), true)
        value = value.replace("\\/", "/")
            .replace("\\u0026", "&")
            .replace("\\u003d", "=")
            .trim('"', '\'', '`')

        if (value.contains("%3A%2F%2F", ignoreCase = true) ||
            value.contains("%3a%2f%2f", ignoreCase = true)
        ) {
            value = runCatching { URLDecoder.decode(value, StandardCharsets.UTF_8.name()) }
                .getOrDefault(value)
        }

        if (value.startsWith("//")) value = "https:$value"
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            value = resolveUrl(value, baseUrl)
        }
        return value.takeIf { isHttpUrl(it) }
    }

    private fun resolveUrl(raw: String, baseUrl: String): String {
        if (raw.startsWith("http://") || raw.startsWith("https://")) return raw
        return runCatching { URI(baseUrl).resolve(raw).toString() }.getOrDefault(raw)
    }

    private fun isHttpUrl(value: String): Boolean =
        value.startsWith("http://") || value.startsWith("https://")

    private fun isDirectMediaUrl(value: String): Boolean {
        val path = runCatching { URI(value).path.orEmpty() }.getOrDefault(value)
        return Regex("""\.(m3u8|mpd|mp4|webm|mov|m4v)$""", RegexOption.IGNORE_CASE)
            .containsMatchIn(path)
    }

    private fun encodePathSegment(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.toString()).replace("+", "%20")
}

private data class JsonVideoCandidate(
    val filename: String,
    val formatRank: Int
)
