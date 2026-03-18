package com.mintanable.notethepad.feature_note.presentation.notes.util

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val NOTE_ID = "NOTE_ID"
        const val NOTE_TITLE = "NOTE_TITLE"
        const val NOTE_CONTENT = "NOTE_CONTENT"
        const val CHANNEL_ID = "note_reminders"
        const val TARGET_NOTE_ID = "TARGET_NOTE_ID"
        const val LAUNCH_EDIT_SCREEN = "LAUNCH_EDIT_SCREEN"
    }

    @SuppressLint("ServiceCast")
    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getLongExtra(NOTE_ID, -1L)
        val noteTitle = intent.getStringExtra(NOTE_TITLE) ?: "Note Reminder"
        val noteContent = intent.getStringExtra(NOTE_CONTENT) ?: ""

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(CHANNEL_ID, "Note Reminders", NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)

        // The Intent will need to target MainActivity in the :app module.
        // For now, we use a string-based component name or an action to keep modules linearly dependent.
        val clickIntent = Intent().apply {
            setClassName(context.packageName, "com.mintanable.notethepad.MainActivity")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(TARGET_NOTE_ID, noteId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            noteId.toInt(),
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Fallback icon
            .setContentTitle(noteTitle)
            .setContentText(noteContent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(noteId.toInt(), notification)
    }
}
