package com.nonton.app.data.provider

import com.nonton.app.data.model.Media
import com.nonton.app.data.model.MediaCategory
import com.nonton.app.data.model.MediaListResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Selects the highest-priority enabled JSON provider and delegates all data
 * operations to it. There is intentionally no mock/demo fallback.
 */
class DynamicMediaProvider(
    private val configBasedProvider: ConfigBasedProvider,
    private val downloadManager: ProviderDownloadManager
) : MediaProvider {

    private val initializationMutex = Mutex()
    private var initialized = false

    @Volatile
    private var activeProvider: MediaProvider = configBasedProvider

    override val providerName: String
        get() = activeProvider.providerName

    override val providerId: String
        get() = activeProvider.providerId

    private suspend fun ensureInitialized() {
        if (initialized) return
        initializationMutex.withLock {
            if (!initialized) {
                reloadActiveProvider()
                initialized = true
            }
        }
    }

    /**
     * Reloads the active provider from bundled, refreshed, and user-created
     * JSON configurations. Safe to call after a manual provider sync.
     */
    suspend fun reloadActiveProvider(): ProviderConfig? = withContext(Dispatchers.IO) {
        val activeConfig = downloadManager.loadAllProviders()
            .firstOrNull { it.isUsable }

        if (activeConfig != null) {
            configBasedProvider.setConfig(activeConfig)
            activeProvider = configBasedProvider
        } else {
            configBasedProvider.setConfig(
                ProviderConfig(
                    id = "unconfigured",
                    name = "Belum ada provider",
                    baseUrl = "",
                    isEnabled = false
                )
            )
            activeProvider = configBasedProvider
        }
        activeConfig
    }

    override suspend fun getMediaList(
        category: MediaCategory,
        page: Int
    ): MediaListResponse = withContext(Dispatchers.IO) {
        ensureInitialized()
        activeProvider.getMediaList(category, page)
    }

    override suspend fun searchMedia(
        query: String,
        page: Int
    ): MediaListResponse = withContext(Dispatchers.IO) {
        ensureInitialized()
        activeProvider.searchMedia(query, page)
    }

    override suspend fun getMediaDetail(mediaId: String): Media? = withContext(Dispatchers.IO) {
        ensureInitialized()
        activeProvider.getMediaDetail(mediaId)
    }

    override suspend fun getVideoUrl(mediaId: String): String? = withContext(Dispatchers.IO) {
        ensureInitialized()
        activeProvider.getVideoUrl(mediaId)
    }

    override suspend fun getVideoHeaders(): Map<String, String> = withContext(Dispatchers.IO) {
        ensureInitialized()
        activeProvider.getVideoHeaders()
    }

    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        ensureInitialized()
        activeProvider.isAvailable()
    }

    /** Switches to a provider by ID. */
    suspend fun switchProvider(providerId: String): Boolean = withContext(Dispatchers.IO) {
        val target = downloadManager.loadAllProviders()
            .firstOrNull { it.id == providerId && it.isUsable }
            ?: return@withContext false

        configBasedProvider.setConfig(target)
        activeProvider = configBasedProvider
        initialized = true
        true
    }

    fun getAvailableProviders(): List<ProviderConfig> =
        downloadManager.loadAllProviders()
}
