package com.nonton.app.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.nonton.app.util.Constants
import com.nonton.app.R
import com.nonton.app.ui.main.MainActivity

class DownloadCompleteReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        when (action) {
            ACTION_DOWNLOAD_COMPLETE -> {
                val downloadId = intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1L)
                if (downloadId != -1L) {
                    onDownloadComplete(context, downloadId)
                }
            }
            ACTION_DOWNLOAD_NOTIFICATION_CLICKED -> {
                val downloadId = intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1L)
                if (downloadId != -1L) {
                    onDownloadNotificationClicked(context, downloadId)
                }
            }
        }
    }

    private fun onDownloadComplete(context: Context, downloadId: Long) {
        // Check download status
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
        val query = android.app.DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)

        if (cursor.moveToFirst()) {
            val status = cursor.getInt(cursor.getColumnIndex(android.app.DownloadManager.COLUMN_STATUS))
            val title = cursor.getString(cursor.getColumnIndex(android.app.DownloadManager.COLUMN_TITLE))
            val localUri = cursor.getString(cursor.getColumnIndex(android.app.DownloadManager.COLUMN_LOCAL_URI))

            when (status) {
                android.app.DownloadManager.STATUS_SUCCESSFUL -> {
                    showCompletionNotification(context, downloadId, title ?: "Download", true, localUri)
                }
                android.app.DownloadManager.STATUS_FAILED -> {
                    val reason = cursor.getInt(cursor.getColumnIndex(android.app.DownloadManager.COLUMN_REASON))
                    showCompletionNotification(context, downloadId, title ?: "Download", false, null)
                }
            }
        }
        cursor.close()
    }

    private fun onDownloadNotificationClicked(context: Context, downloadId: Long) {
        // Open the downloaded file or navigate to downloads screen
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("navigate_to", "downloads")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
    }

    private fun showCompletionNotification(
        context: Context,
        downloadId: Long,
        title: String,
        success: Boolean,
        localUri: String?
    ) {
        val channelId = Constants.CHANNEL_DOWNLOAD_ID
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Downloads",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Download completion notifications"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("navigate_to", "downloads")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_download_done)
            .setContentTitle(if (success) "Download Complete" else "Download Failed")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(title))

        if (success) {
            builder.setColor(ContextCompat.getColor(context, R.color.status_success))
        } else {
            builder.setColor(ContextCompat.getColor(context, R.color.status_error))
        }

        NotificationManagerCompat.from(context).notify(
            downloadId.hashCode(),
            builder.build()
        )
    }

    companion object {
        const val ACTION_DOWNLOAD_COMPLETE = "android.intent.action.DOWNLOAD_COMPLETE"
        const val ACTION_DOWNLOAD_NOTIFICATION_CLICKED =
            "android.intent.action.DOWNLOAD_NOTIFICATION_CLICKED"
        const val EXTRA_DOWNLOAD_ID = "download_id"
    }
}