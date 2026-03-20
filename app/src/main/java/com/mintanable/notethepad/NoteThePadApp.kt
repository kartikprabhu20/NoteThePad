package com.mintanable.notethepad

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.mintanable.notethepad.core.model.NoteThePadConstants.BACKUP_NOTIFICATION_CHANNEL_ID
import com.mintanable.notethepad.core.model.NoteThePadConstants.DOWNLOAD_MODEL_NOTIFICATION_CHANNEL_ID
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class NoteThePadApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val backupChannel = NotificationChannel(
            BACKUP_NOTIFICATION_CHANNEL_ID,
            "Backup Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows progress of Google Drive backups"
        }

        val downloadChannel = NotificationChannel(
            DOWNLOAD_MODEL_NOTIFICATION_CHANNEL_ID,
            "Model Downloads",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows progress of AI model downloads"
        }

        notificationManager.createNotificationChannels(listOf(backupChannel, downloadChannel))
    }
}