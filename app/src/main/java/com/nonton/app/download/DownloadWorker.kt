package com.nonton.app.download

import android.content.Context
import android.net.Uri
import androidx.lifecycle.map
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nonton.app.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

class DownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val mediaId = inputData.getString("media_id")
            ?: return@withContext Result.failure()
        val videoUrl = inputData.getString("video_url")
            ?: return@withContext Result.failure()
        val title = inputData.getString("title") ?: "Unknown"
        val quality = inputData.getString("quality") ?: Constants.DEFAULT_DOWNLOAD_QUALITY
        val season = inputData.getInt("season", -1)
        val episode = inputData.getInt("episode", -1)

        try {
            // Create download directory
            val downloadDir = File(applicationContext.getExternalFilesDir(null), "downloads")
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }

            // Generate filename
            val fileName = "${mediaId}_${quality}.mp4"
            val destinationFile = File(downloadDir, fileName)

            // Download with progress updates
            downloadVideo(videoUrl, destinationFile) { progress ->
                setProgress(
                    androidx.work.Data.Builder()
                        .putString("media_id", mediaId)
                        .putFloat("progress", progress)
                        .build()
                )
            }

            // Save download metadata to database
            saveDownloadMetadata(mediaId, destinationFile, title, quality, season, episode)

            // Notify completion
            notifyDownloadComplete(mediaId)

            Result.success()
        } catch (e: Exception) {
            // Update status to failed
            setProgress(
                androidx.work.Data.Builder()
                    .putString("media_id", mediaId)
                    .putFloat("progress", -1f)
                    .putString("error", e.message ?: "Unknown error")
                    .build()
            )
            Result.retry()
        }
    }

    private suspend fun downloadVideo(
        videoUrl: String,
        destination: File,
        progressListener: suspend (Float) -> Unit
    ) {
        val connection = URL(videoUrl).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 30000
        connection.readTimeout = 30000
        connection.connect()

        val contentLength = connection.contentLength.toLong()
        if (contentLength <= 0) {
            throw IOException("Invalid content length")
        }

        val input = connection.inputStream
        val output = FileOutputStream(destination)

        val buffer = ByteArray(8192)
        var totalRead = 0L

        try {
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break

                output.write(buffer, 0, read)
                totalRead += read

                val progress = totalRead.toFloat() / contentLength
                progressListener(progress)

                // Yield to allow cancellation check
                if (isStopped) {
                    throw InterruptedException("Download cancelled")
                }
            }
        } finally {
            input.close()
            output.close()
            connection.disconnect()
        }
    }

    private fun saveDownloadMetadata(
        mediaId: String,
        file: File,
        title: String,
        quality: String,
        season: Int,
        episode: Int
    ) {
        // TODO: Save to Room database
        // DownloadEntity(
        //     mediaId = mediaId,
        //     title = title,
        //     filePath = file.absolutePath,
        //     quality = quality,
        //     fileSize = file.length(),
        //     downloadDate = System.currentTimeMillis(),
        //     season = season,
        //     episode = episode
        // )
    }

    private fun notifyDownloadComplete(mediaId: String) {
        // Send local notification
        // TODO: Implement notification
    }
}

class DownloadManager(
    private val context: Context
) {
    private val workManager = androidx.work.WorkManager.getInstance(context)

    fun startDownload(
        mediaId: String,
        videoUrl: String,
        title: String,
        quality: String = Constants.DEFAULT_DOWNLOAD_QUALITY,
        season: Int = -1,
        episode: Int = -1
    ) {
        val workRequest = androidx.work.OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(
                androidx.work.Data.Builder()
                    .putString("media_id", mediaId)
                    .putString("video_url", videoUrl)
                    .putString("title", title)
                    .putString("quality", quality)
                    .putInt("season", season)
                    .putInt("episode", episode)
                    .build()
            )
            .addTag(Constants.WORK_TAG_DOWNLOAD)
            .setConstraints(
                androidx.work.Constraints.Builder()
                    .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                    .setRequiresCharging(false)
                    .setRequiresStorageNotLow(true)
                    .build()
            )
            .build()

        workManager.enqueueUniqueWork(
            "download_$mediaId",
            androidx.work.ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun pauseDownload(mediaId: String) {
        // WorkManager has no pause primitive. Cancelling keeps the unique work
        // name consistent and allows a later download request to restart it.
        workManager.cancelUniqueWork("download_$mediaId")
    }

    fun cancelDownload(mediaId: String) {
        workManager.cancelUniqueWork("download_$mediaId")
    }

    fun getDownloadProgress(mediaId: String): androidx.lifecycle.LiveData<androidx.work.WorkInfo?> {
        return workManager.getWorkInfosByTagLiveData(Constants.WORK_TAG_DOWNLOAD)
            .map { list ->
                list.firstOrNull { it.id.toString() == mediaId }
            }
    }

    fun getAllDownloads(): androidx.lifecycle.LiveData<List<androidx.work.WorkInfo>> {
        return workManager.getWorkInfosByTagLiveData(Constants.WORK_TAG_DOWNLOAD)
    }

    fun clearCompletedDownloads() {
        workManager.pruneWork()
    }
}