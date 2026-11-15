package com.nonton.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nonton.app.data.provider.DynamicMediaProvider
import com.nonton.app.data.provider.ProviderConfig
import com.nonton.app.data.provider.ProviderDownloadManager
import com.nonton.app.data.provider.ProviderSyncOutcome
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ProviderSettingsViewModel @Inject constructor(
    private val downloadManager: ProviderDownloadManager,
    private val dynamicMediaProvider: DynamicMediaProvider
) : ViewModel() {

    private val _providers = MutableStateFlow(listOf<ProviderConfig>())
    val providers = _providers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<ProviderSettingsMessage?>(null)
    val message = _message.asStateFlow()

    init {
        loadProviders()
    }

    fun loadProviders() {
        _providers.value = downloadManager.loadAllProviders()
    }

    fun downloadProviders() {
        if (_isLoading.value) return
        _isLoading.value = true
        _message.value = null

        viewModelScope.launch {
            try {
                val result = downloadManager.downloadProviders()
                dynamicMediaProvider.reloadActiveProvider()
                loadProviders()

                val details = result.failures.joinToString(separator = "\n") {
                    "${it.providerName}: ${it.message}"
                }
                _message.value = ProviderSettingsMessage(
                    text = listOfNotNull(result.message, details.takeIf { it.isNotBlank() })
                        .joinToString(separator = "\n\n"),
                    isError = result.outcome == ProviderSyncOutcome.FAILURE ||
                        result.outcome == ProviderSyncOutcome.PARTIAL_FAILURE
                )
            } catch (e: Exception) {
                _message.value = ProviderSettingsMessage(
                    text = "Sinkronisasi gagal: ${e.message ?: e.javaClass.simpleName}",
                    isError = true
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveCustomProvider(
        id: String,
        name: String,
        baseUrl: String,
        downloadUrl: String,
        isEnabled: Boolean
    ) {
        if (id.isBlank() || name.isBlank() || (baseUrl.isBlank() && downloadUrl.isBlank())) {
            _message.value = ProviderSettingsMessage(
                text = "ID, nama, dan Base URL atau URL JSON provider wajib diisi.",
                isError = true
            )
            return
        }

        val provider = ProviderConfig(
            id = id.trim(),
            name = name.trim(),
            baseUrl = baseUrl.trim().trimEnd('/'),
            downloadUrl = downloadUrl.trim().ifBlank { null },
            isEnabled = isEnabled,
            priority = 200
        )

        downloadManager.saveCustomProvider(provider)
        viewModelScope.launch { dynamicMediaProvider.reloadActiveProvider() }
        loadProviders()
        _message.value = ProviderSettingsMessage(
            text = if (baseUrl.isBlank()) {
                "Provider '$name' disimpan. Tekan Sinkronisasi untuk mengunduh konfigurasi JSON."
            } else {
                "Provider '$name' berhasil ditambahkan."
            },
            isError = false
        )
    }

    fun toggleProvider(provider: ProviderConfig, isEnabled: Boolean) {
        val updated = provider.copy(
            isEnabled = isEnabled,
            priority = maxOf(provider.priority, 200)
        )
        downloadManager.saveCustomProvider(updated)
        viewModelScope.launch { dynamicMediaProvider.reloadActiveProvider() }
        loadProviders()
    }

    fun deleteCustomProvider(providerId: String) {
        downloadManager.deleteCustomProvider(providerId)
        viewModelScope.launch { dynamicMediaProvider.reloadActiveProvider() }
        loadProviders()
        _message.value = ProviderSettingsMessage(
            text = "Provider dihapus.",
            isError = false
        )
    }

    fun clearMessage() {
        _message.value = null
    }
}

data class ProviderSettingsMessage(
    val text: String,
    val isError: Boolean
)

@Composable
fun ProviderSettingsScreen(
    onBackClick: () -> Unit
) {
    val viewModel: ProviderSettingsViewModel = hiltViewModel()
    val providers by viewModel.providers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val message by viewModel.message.collectAsState()
    val hasActiveProvider = providers.any { it.isUsable }

    var newProviderId by remember { mutableStateOf("") }
    var newProviderName by remember { mutableStateOf("") }
    var newProviderBaseUrl by remember { mutableStateOf("") }
    var newProviderDownloadUrl by remember { mutableStateOf("") }
    var newProviderEnabled by remember { mutableStateOf(true) }
    var showAddForm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pengaturan Provider",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineMedium
            )
            TextButton(onClick = onBackClick) {
                Text("Kembali")
            }
        }

        Text(
            text = "Tambahkan URL JSON provider untuk mengambil konfigurasi scraper. Setelah URL disimpan, tekan Sinkronisasi. Provider juga dapat memakai Base URL dan selector bawaan.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (showAddForm) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Tambah Provider Baru",
                        style = MaterialTheme.typography.titleMedium
                    )
                    OutlinedTextField(
                        value = newProviderId,
                        onValueChange = { newProviderId = it },
                        label = { Text("ID Provider") },
                        placeholder = { Text("contoh: provider_saya") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )
                    OutlinedTextField(
                        value = newProviderName,
                        onValueChange = { newProviderName = it },
                        label = { Text("Nama Provider") },
                        placeholder = { Text("contoh: Sumber Film") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newProviderBaseUrl,
                        onValueChange = { newProviderBaseUrl = it },
                        label = { Text("Base URL situs (opsional jika ada JSON)") },
                        placeholder = { Text("https://contoh.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                    )
                    OutlinedTextField(
                        value = newProviderDownloadUrl,
                        onValueChange = { newProviderDownloadUrl = it },
                        label = { Text("URL JSON provider (opsional)") },
                        placeholder = { Text("https://contoh.com/provider.json") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Aktif secara default",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Switch(
                            checked = newProviderEnabled,
                            onCheckedChange = { newProviderEnabled = it }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showAddForm = false }) {
                            Text("Batal")
                        }
                        FilledTonalButton(
                            onClick = {
                                viewModel.saveCustomProvider(
                                    id = newProviderId,
                                    name = newProviderName,
                                    baseUrl = newProviderBaseUrl,
                                    downloadUrl = newProviderDownloadUrl,
                                    isEnabled = newProviderEnabled
                                )
                                showAddForm = false
                                newProviderId = ""
                                newProviderName = ""
                                newProviderBaseUrl = ""
                                newProviderDownloadUrl = ""
                                newProviderEnabled = true
                            }
                        ) {
                            Text("Simpan")
                        }
                    }
                }
            }
        }

        Text(
            text = "Provider yang tersedia",
            style = MaterialTheme.typography.titleMedium
        )

        if (!hasActiveProvider) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "Belum ada provider aktif. Tambahkan URL JSON provider atau Base URL terlebih dahulu.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        providers.forEach { provider ->
            val isCustom = provider.priority >= 200
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = provider.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "${provider.id} • v${provider.version}${if (isCustom) " • Custom" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = provider.isEnabled,
                            onCheckedChange = { viewModel.toggleProvider(provider, it) }
                        )
                        if (isCustom) {
                            TextButton(onClick = { viewModel.deleteCustomProvider(provider.id) }) {
                                Text("Hapus")
                            }
                        }
                    }
                    Text(
                        text = provider.baseUrl.ifBlank { "Base URL diambil dari JSON provider" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    provider.downloadUrl?.takeIf { it.isNotBlank() }?.let { downloadUrl ->
                        Text(
                            text = "JSON: $downloadUrl",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { showAddForm = true }) {
                Text("+ Tambah Provider")
            }
            FilledTonalButton(
                onClick = viewModel::downloadProviders,
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        strokeWidth = 2.dp
                    )
                }
                Text(if (isLoading) "Menyinkronkan…" else "Sinkronisasi")
            }
        }

        message?.let { currentMessage ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = if (currentMessage.isError) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                }
            ) {
                Text(
                    text = currentMessage.text,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (currentMessage.isError) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    }
                )
            }
        }

        Text(
            text = "Pastikan Anda memiliki izin untuk mengakses konten dari situs target dan complies dengan ketentuan layanan situs tersebut.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
