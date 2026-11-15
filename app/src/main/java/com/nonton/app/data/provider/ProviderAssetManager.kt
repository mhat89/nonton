package com.nonton.app.data.provider

import android.content.Context
import android.util.Log
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Memuat konfigurasi provider dari folder assets/app/src/main/assets/providers/.
 * Ini memungkinkan app bekerja offline tanpa perlu download dari server.
 */
class ProviderAssetManager(private val context: Context) {

    companion object {
        private const val TAG = "ProviderAssetManager"
        private const val PROVIDERS_DIR = "providers/"
    }

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Memuat semua provider dari assets.
     */
    fun loadFromAssets(): List<ProviderConfig> {
        val providers = mutableListOf<ProviderConfig>()

        try {
            val assetManager = context.assets
            val files = assetManager.list(PROVIDERS_DIR) ?: emptyArray()

            for (fileName in files) {
                if (fileName.endsWith(".json")) {
                    try {
                        val inputStream = assetManager.open("$PROVIDERS_DIR$fileName")
                        val reader = BufferedReader(InputStreamReader(inputStream))
                        val content = reader.readText()
                        reader.close()
                        inputStream.close()

                        val config = json.decodeFromString(ProviderConfig.serializer(), content)
                        providers.add(config)
                        Log.d(TAG, "Load from assets: ${config.id}")
                    } catch (e: Exception) {
                        Log.w(TAG, "Gagal load $fileName dari assets", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal list direktori assets", e)
        }

        return providers.sortedByDescending { it.priority }
    }
}
