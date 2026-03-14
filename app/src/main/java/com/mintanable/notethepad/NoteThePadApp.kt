package com.mintanable.notethepad

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class NoteThePadApp : Application(), Configuration.Provider {

    companion object{
        const val BACKUP_NOTIFICATION_ID = 1001
        const val DOWNLOAD_NOTIFICATION_ID = 1002
        const val DOWNLOAD_MODEL_NOTIFICATION_ID = 1003

        const val BACKUP_NOTIFICATION_CHANNEL_ID = "backup_channel"
    }

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val name = "Backup Service"
        val descriptionText = "Shows progress of Google Drive backups"
        val importance = NotificationManager.IMPORTANCE_LOW

        val channel = NotificationChannel(BACKUP_NOTIFICATION_CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}