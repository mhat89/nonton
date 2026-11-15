package com.nonton.app.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nonton.app.data.model.VideoSource
import com.nonton.app.domain.usecase.ResolveVideoSourceUseCase
import com.nonton.app.domain.usecase.SyncProviderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val resolveVideoSourceUseCase: ResolveVideoSourceUseCase,
    private val syncProviderUseCase: SyncProviderUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState = _uiState.asStateFlow()

    private var requestedKey: String? = null

    fun resolve(mediaId: String, explicitVideoUrl: String? = null) {
        val key = "$mediaId|${explicitVideoUrl.orEmpty()}"
        if (requestedKey == key && _uiState.value.source != null) return
        requestedKey = key

        if (!explicitVideoUrl.isNullOrBlank()) {
            _uiState.value = PlayerUiState(
                source = VideoSource(explicitVideoUrl),
                isResolving = false
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = PlayerUiState(isResolving = true)
            val result = resolveVideoSourceUseCase(mediaId)
            _uiState.value = result.fold(
                onSuccess = { source ->
                    PlayerUiState(source = source, isResolving = false)
                },
                onFailure = { error ->
                    PlayerUiState(
                        isResolving = false,
                        error = error.message ?: "Gagal mendapatkan URL video dari provider"
                    )
                }
            )
        }
    }

    /** Synchronizes provider JSON links, reloads the provider, then resolves again. */
    fun syncAndRetry(mediaId: String) {
        if (_uiState.value.isResolving) return

        viewModelScope.launch {
            _uiState.value = PlayerUiState(isResolving = true)
            val syncResult = runCatching { syncProviderUseCase() }
            val resolveResult = resolveVideoSourceUseCase(mediaId)

            _uiState.value = resolveResult.fold(
                onSuccess = { source ->
                    PlayerUiState(source = source, isResolving = false)
                },
                onFailure = { resolveError ->
                    val syncDetails = syncResult.getOrNull()?.failures
                        ?.joinToString(separator = "\n") { "${it.providerName}: ${it.message}" }
                        .orEmpty()
                    val combinedMessage = buildString {
                        append(resolveError.message ?: "Gagal mendapatkan URL video dari provider")
                        if (syncDetails.isNotBlank()) {
                            append("\n\nSinkronisasi provider:\n")
                            append(syncDetails)
                        }
                    }
                    PlayerUiState(
                        isResolving = false,
                        error = combinedMessage
                    )
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class PlayerUiState(
    val source: VideoSource? = null,
    val isResolving: Boolean = true,
    val error: String? = null
)
