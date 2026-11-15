package com.nonton.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nonton.app.data.model.Media
import com.nonton.app.data.model.MediaCategory
import com.nonton.app.data.model.MediaListResponse
import com.nonton.app.data.provider.ProviderSyncOutcome
import com.nonton.app.domain.usecase.GetMediaListUseCase
import com.nonton.app.domain.usecase.GetProviderStateUseCase
import com.nonton.app.domain.usecase.SyncProviderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getMediaListUseCase: GetMediaListUseCase,
    private val syncProviderUseCase: SyncProviderUseCase,
    private val getProviderStateUseCase: GetProviderStateUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun loadCategory(category: MediaCategory, page: Int = 1) {
        viewModelScope.launch {
            val result = getMediaListUseCase(category, page)
            _uiState.update { state ->
                state.copy(
                    categoryResults = state.categoryResults + (category to result),
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    /** Pull-to-refresh action. It waits for every provider category request. */
    fun refresh() {
        if (_uiState.value.isRefreshing) return

        _uiState.update { it.copy(isRefreshing = true, error = null) }
        viewModelScope.launch {
            val activeProvider = getProviderStateUseCase()
            if (activeProvider == null) {
                _uiState.update { state ->
                    state.copy(
                        categoryResults = emptyMap(),
                        heroBanner = null,
                        isRefreshing = false,
                        error = null,
                        providerStatus = ProviderStatus.NotConfigured
                    )
                }
                return@launch
            }

            val results = fetchAllCategories()
            val failures = results.values.mapNotNull { it.exceptionOrNull() }
            val hasContent = results.values.any { result ->
                result.getOrNull()?.results?.isNotEmpty() == true
            }
            val allFailed = results.isNotEmpty() && failures.size == results.size

            _uiState.update { state ->
                state.copy(
                    categoryResults = results,
                    heroBanner = results[MediaCategory.POPULAR]?.getOrNull()?.results?.firstOrNull()
                        ?: state.heroBanner,
                    isRefreshing = false,
                    error = if (!hasContent) {
                        failures.firstOrNull()?.message
                    } else {
                        null
                    },
                    providerStatus = if (allFailed) ProviderStatus.Error else ProviderStatus.Ready
                )
            }
        }
    }

    /** Manually synchronizes provider JSON links and reloads active content. */
    fun syncProvider() {
        if (_uiState.value.syncState.status == SyncStatus.Loading) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRefreshing = true,
                    syncState = ProviderSyncState(
                        status = SyncStatus.Loading,
                        message = "Mengambil konfigurasi dari URL provider…"
                    )
                )
            }

            val providerResult = runCatching { syncProviderUseCase() }
            val providerReport = providerResult.getOrNull()
            val activeProvider = providerReport?.activeProvider ?: getProviderStateUseCase()
            val categoryResults = if (activeProvider != null) {
                fetchAllCategories()
            } else {
                emptyMap()
            }
            val categoryFailures = categoryResults.values.mapNotNull { it.exceptionOrNull() }

            val details = buildList {
                providerReport?.failures?.forEach { failure ->
                    add("${failure.providerName}: ${failure.message}")
                }
                categoryFailures.forEach { error ->
                    add(error.message ?: "Gagal mengambil konten dari provider")
                }
            }.distinct()

            val hasProviderFailure = providerResult.isFailure ||
                providerReport?.failures?.isNotEmpty() == true
            val syncStatus = when {
                hasProviderFailure -> SyncStatus.Error
                activeProvider == null -> SyncStatus.NeedsConfiguration
                categoryFailures.isNotEmpty() -> SyncStatus.Error
                else -> SyncStatus.Success
            }
            val providerStatus = when {
                activeProvider == null -> ProviderStatus.NotConfigured
                categoryResults.isNotEmpty() && categoryFailures.size == categoryResults.size -> {
                    ProviderStatus.Error
                }
                else -> ProviderStatus.Ready
            }

            _uiState.update { state ->
                state.copy(
                    categoryResults = categoryResults,
                    heroBanner = categoryResults[MediaCategory.POPULAR]?.getOrNull()?.results?.firstOrNull()
                        ?: state.heroBanner,
                    isRefreshing = false,
                    error = if (activeProvider != null && categoryFailures.isNotEmpty()) {
                        categoryFailures.first().message
                    } else {
                        null
                    },
                    providerStatus = providerStatus,
                    syncState = ProviderSyncState(
                        status = syncStatus,
                        message = buildSyncMessage(
                            providerResult = providerResult,
                            categoryFailures = categoryFailures,
                            hasActiveProvider = activeProvider != null
                        ),
                        details = details,
                        outcome = providerReport?.outcome
                    )
                )
            }
        }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTabIndex = index.coerceIn(0, 3)) }
    }

    fun dismissSyncState() {
        _uiState.update { it.copy(syncState = ProviderSyncState()) }
    }

    private suspend fun fetchAllCategories(): Map<MediaCategory, Result<MediaListResponse>> =
        coroutineScope {
            HOME_CATEGORIES.map { category ->
                async { category to getMediaListUseCase(category, 1) }
            }.awaitAll().toMap()
        }

    private fun buildSyncMessage(
        providerResult: Result<com.nonton.app.data.provider.ProviderDownloadResult>,
        categoryFailures: List<Throwable>,
        hasActiveProvider: Boolean
    ): String {
        providerResult.exceptionOrNull()?.let { error ->
            return "Sinkronisasi gagal: ${error.message ?: error.javaClass.simpleName}"
        }

        val provider = providerResult.getOrNull()
        return when {
            provider?.failures?.isNotEmpty() == true ->
                provider.message
            !hasActiveProvider ->
                provider?.message?.takeIf { it.isNotBlank() }
                    ?: "Belum ada provider aktif. Tambahkan URL JSON provider di Pengaturan Provider."
            categoryFailures.isNotEmpty() ->
                "Provider aktif, tetapi konten gagal dimuat. Periksa koneksi atau konfigurasi provider."
            provider != null ->
                provider.message
            else ->
                "Sinkronisasi provider selesai."
        }
    }

    data class UiState(
        val categoryResults: Map<MediaCategory, Result<MediaListResponse>> = emptyMap(),
        val heroBanner: Media? = null,
        val continueWatching: List<Media> = emptyList(),
        val isRefreshing: Boolean = false,
        val error: String? = null,
        val providerStatus: ProviderStatus = ProviderStatus.Loading,
        val selectedTabIndex: Int = 0,
        val syncState: ProviderSyncState = ProviderSyncState()
    )

    enum class ProviderStatus {
        Loading,
        Ready,
        NotConfigured,
        Error
    }

    companion object {
        private val HOME_CATEGORIES = listOf(
            MediaCategory.POPULAR,
            MediaCategory.POPULAR_SERIES,
            MediaCategory.POPULAR_DRAKOR,
            MediaCategory.POPULAR_ANIME
        )
    }
}

enum class SyncStatus {
    Idle,
    Loading,
    Success,
    NeedsConfiguration,
    Error
}

data class ProviderSyncState(
    val status: SyncStatus = SyncStatus.Idle,
    val message: String = "",
    val details: List<String> = emptyList(),
    val outcome: ProviderSyncOutcome? = null
)
