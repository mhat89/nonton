package com.nonton.app.data.provider

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray

/**
 * Loads provider configurations and refreshes user-supplied provider JSON links.
 *
 * Resolution order (same ID):
 * 1. User-created or user-overridden entries stored in SharedPreferences
 * 2. Refreshed JSON files stored in the app's private files directory
 * 3. Bundled JSON files in `app/src/main/assets/providers/`
 */
class ProviderDownloadManager(
    private val context: Context
) {

    companion object {
        private const val TAG = "ProviderDownloadManager"
        private const val PREF_CUSTOM_PROVIDERS = "custom_providers"
        private const val PROVIDERS_DIR = "providers"
        private const val ASSETS_INDEX = "providers/index.json"
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val client = okhttp3.OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    /** Provider configurations bundled with the application. */
    val defaultProviders: List<ProviderConfig>
        get() = loadFromAssets()

    /**
     * Synchronizes every enabled provider that has a `downloadUrl`.
     *
     * The result is intentionally truthful: a failed remote request is reported
     * as a failure instead of being presented as an offline success.
     */
    suspend fun downloadProviders(): ProviderDownloadResult = withContext(Dispatchers.IO) {
        val customProviders = loadCustomProviders()
        val customIds = customProviders.map { it.id }.toSet()
        val sources = (customProviders + loadLocalProviders() + defaultProviders)
            .filter { it.isEnabled && !it.downloadUrl.isNullOrBlank() }
            .distinctBy { it.id }

        if (sources.isEmpty()) {
            val activeProvider = loadActiveProvider()
            return@withContext ProviderDownloadResult(
                success = activeProvider != null,
                config = activeProvider,
                activeProvider = activeProvider,
                message = if (activeProvider != null) {
                    "Tidak ada URL JSON untuk disinkronkan. Provider aktif '${activeProvider.name}' tetap digunakan."
                } else {
                    "Belum ada provider aktif. Tambahkan URL JSON provider di Pengaturan Provider."
                },
                outcome = if (activeProvider != null) {
                    ProviderSyncOutcome.NO_REMOTE_SOURCES
                } else {
                    ProviderSyncOutcome.NO_PROVIDER_CONFIGURED
                },
                remoteSourceCount = 0
            )
        }

        val downloaded = mutableListOf<ProviderConfig>()
        val failures = mutableListOf<ProviderSyncFailure>()

        for (source in sources) {
            try {
                val remoteConfigs = downloadConfig(source)
                if (remoteConfigs.isEmpty()) {
                    throw IllegalStateException("JSON provider tidak berisi konfigurasi yang valid")
                }

                remoteConfigs.forEach { remote ->
                    val merged = mergeWithSource(remote, source)
                    saveDownloadedProvider(merged, updateCustom = source.id in customIds)
                    downloaded += merged
                }
            } catch (e: Exception) {
                val reason = e.message?.takeIf { it.isNotBlank() } ?: e.javaClass.simpleName
                Log.w(TAG, "Gagal sinkronkan provider ${source.id}", e)
                failures += ProviderSyncFailure(
                    providerId = source.id,
                    providerName = source.name,
                    message = reason
                )
            }
        }

        val activeProvider = loadActiveProvider()
        val success = failures.isEmpty()
        val outcome = when {
            success -> ProviderSyncOutcome.SUCCESS
            downloaded.isNotEmpty() -> ProviderSyncOutcome.PARTIAL_FAILURE
            else -> ProviderSyncOutcome.FAILURE
        }
        val message = when {
            success && downloaded.isNotEmpty() ->
                "Berhasil menyinkronkan ${downloaded.size} konfigurasi provider."
            success -> "Konfigurasi provider lokal siap digunakan."
            downloaded.isNotEmpty() ->
                "Sinkronisasi berhasil untuk ${downloaded.size} konfigurasi, tetapi ${failures.size} sumber gagal."
            else -> "Sinkronisasi provider gagal untuk ${failures.size} dari ${sources.size} sumber."
        }

        ProviderDownloadResult(
            success = success,
            config = activeProvider,
            activeProvider = activeProvider,
            message = message,
            outcome = outcome,
            remoteSourceCount = sources.size,
            downloadedCount = downloaded.size,
            failedCount = failures.size,
            downloadedProviders = downloaded,
            failures = failures
        )
    }

    /**
     * Loads all known providers. A user entry wins over a bundled entry with
     * the same ID, while a refreshed local configuration can replace its
     * bundled counterpart.
     */
    fun loadAllProviders(): List<ProviderConfig> {
        val providers = loadCustomProviders() + loadLocalProviders() + defaultProviders
        return providers
            .distinctBy { it.id }
            .sortedWith(
                compareByDescending<ProviderConfig> { it.isUsable }
                    .thenByDescending { it.priority }
                    .thenByDescending { it.version }
            )
    }

    /** Returns the highest-priority provider that can currently be used. */
    fun loadActiveProvider(): ProviderConfig? =
        loadAllProviders().firstOrNull { it.isUsable }

    /** Persists a user-created provider entry. */
    fun saveCustomProvider(provider: ProviderConfig) {
        val prefs = customPreferences()
        val providerJson = json.encodeToString(ProviderConfig.serializer(), provider)
        prefs.edit().putString(provider.id, providerJson).apply()
        Log.d(TAG, "Custom provider disimpan: ${provider.id}")
    }

    /** Removes a user-created provider entry. */
    fun deleteCustomProvider(providerId: String) {
        customPreferences().edit().remove(providerId).apply()
        Log.d(TAG, "Custom provider dihapus: $providerId")
    }

    fun needsUpdate(): Boolean = false

    private fun downloadConfig(source: ProviderConfig): List<ProviderConfig> {
        val downloadUrl = source.downloadUrl?.trim().orEmpty()
        require(downloadUrl.isNotBlank()) { "URL provider kosong" }

        val request = okhttp3.Request.Builder()
            .url(downloadUrl)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json, text/plain, */*")
            .apply {
                source.headers.forEach { (name, value) -> header(name, value) }
            }
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP ${response.code} ${response.message}")
            }
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) throw IllegalStateException("Respons provider kosong")
            return parseProviderConfigs(body)
        }
    }

    private fun parseProviderConfigs(body: String): List<ProviderConfig> {
        try {
            return json.decodeFromString<List<ProviderConfig>>(body)
        } catch (_: SerializationException) {
            // The response may be a single provider object.
        }

        val element = try {
            json.parseToJsonElement(body)
        } catch (e: Exception) {
            throw IllegalStateException("Respons bukan JSON provider yang valid: ${e.message}")
        }

        return when (element) {
            is JsonArray -> element.map { item ->
                json.decodeFromJsonElement(ProviderConfig.serializer(), item)
            }
            is JsonObject -> decodeProviderObject(element)
            else -> emptyList()
        }
    }

    private fun decodeProviderObject(root: JsonObject): List<ProviderConfig> {
        val providerElement = root["provider"]
            ?: root["providers"]
            ?: root["data"]
            ?: root

        return when (providerElement) {
            is JsonArray -> providerElement.jsonArray.map { item ->
                json.decodeFromJsonElement(ProviderConfig.serializer(), item)
            }
            is JsonObject -> listOf(
                json.decodeFromJsonElement(ProviderConfig.serializer(), providerElement)
            )
            else -> emptyList()
        }
    }

    private fun mergeWithSource(remote: ProviderConfig, source: ProviderConfig): ProviderConfig {
        val id = remote.id.takeIf { it.isNotBlank() } ?: source.id
        val name = remote.name.takeIf { it.isNotBlank() } ?: source.name
        val baseUrl = remote.baseUrl.takeIf { it.isNotBlank() } ?: source.baseUrl

        if (id.isBlank()) throw IllegalStateException("Provider JSON tidak memiliki id")
        if (name.isBlank()) throw IllegalStateException("Provider JSON tidak memiliki name")
        if (baseUrl.isBlank()) throw IllegalStateException("Provider JSON tidak memiliki baseUrl")

        return remote.copy(
            id = id,
            name = name,
            baseUrl = baseUrl.trimEnd('/'),
            downloadUrl = remote.downloadUrl ?: source.downloadUrl,
            isEnabled = remote.isEnabled && source.isEnabled,
            priority = maxOf(remote.priority, source.priority),
            version = maxOf(remote.version, source.version)
        )
    }

    private fun saveDownloadedProvider(provider: ProviderConfig, updateCustom: Boolean) {
        val providersDir = File(context.filesDir, PROVIDERS_DIR)
        if (!providersDir.exists()) providersDir.mkdirs()

        val safeId = provider.id.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val providerFile = File(providersDir, "$safeId.json")
        providerFile.writeText(
            json.encodeToString(ProviderConfig.serializer(), provider)
        )

        if (updateCustom) {
            saveCustomProvider(provider)
        }
    }

    private fun loadFromAssets(): List<ProviderConfig> {
        val providers = mutableListOf<ProviderConfig>()

        try {
            val indexStream = context.assets.open(ASSETS_INDEX)
            val indexContent = indexStream.bufferedReader().use { it.readText() }
            providers += json.decodeFromString<List<ProviderConfig>>(indexContent)
            Log.d(TAG, "Memuat ${providers.size} provider dari assets/index.json")
        } catch (e: Exception) {
            Log.w(TAG, "Gagal membaca assets/index.json; mencoba file individual", e)
            val files = context.assets.list("providers/") ?: emptyArray()
            for (fileName in files) {
                if (!fileName.endsWith(".json") || fileName == "index.json") continue
                try {
                    val content = context.assets.open("providers/$fileName")
                        .bufferedReader()
                        .use { it.readText() }
                    providers += json.decodeFromString<ProviderConfig>(content)
                } catch (ex: Exception) {
                    Log.w(TAG, "Gagal memuat assets/providers/$fileName", ex)
                }
            }
        }

        return providers.sortedByDescending { it.priority }
    }

    private fun loadLocalProviders(): List<ProviderConfig> {
        val providersDir = File(context.filesDir, PROVIDERS_DIR)
        if (!providersDir.exists()) return emptyList()

        return providersDir.listFiles()
            .orEmpty()
            .filter { it.isFile && it.extension.equals("json", ignoreCase = true) }
            .mapNotNull { file ->
                try {
                    json.decodeFromString<ProviderConfig>(file.readText())
                } catch (e: Exception) {
                    Log.w(TAG, "Gagal memuat ${file.name} dari penyimpanan lokal", e)
                    null
                }
            }
    }

    private fun loadCustomProviders(): List<ProviderConfig> {
        val providers = mutableListOf<ProviderConfig>()
        customPreferences().all.forEach { (key, value) ->
            val jsonString = value as? String ?: return@forEach
            try {
                providers += json.decodeFromString<ProviderConfig>(jsonString)
            } catch (e: Exception) {
                Log.w(TAG, "Gagal memuat custom provider $key", e)
            }
        }
        return providers.sortedByDescending { it.priority }
    }

    private fun customPreferences(): SharedPreferences =
        context.getSharedPreferences(PREF_CUSTOM_PROVIDERS, Context.MODE_PRIVATE)
}

/** Result of a provider configuration synchronization. */
data class ProviderDownloadResult(
    val success: Boolean,
    /** The active provider after synchronization, if one is usable. */
    val config: ProviderConfig?,
    val activeProvider: ProviderConfig? = config,
    val message: String = "",
    val outcome: ProviderSyncOutcome = ProviderSyncOutcome.SUCCESS,
    val remoteSourceCount: Int = 0,
    val downloadedCount: Int = 0,
    val failedCount: Int = 0,
    val downloadedProviders: List<ProviderConfig> = emptyList(),
    val failures: List<ProviderSyncFailure> = emptyList()
)

/** Structured outcome of a provider synchronization attempt. */
enum class ProviderSyncOutcome {
    SUCCESS,
    NO_REMOTE_SOURCES,
    NO_PROVIDER_CONFIGURED,
    PARTIAL_FAILURE,
    FAILURE
}

/** Details for one failed provider link. */
data class ProviderSyncFailure(
    val providerId: String,
    val providerName: String,
    val message: String
)
