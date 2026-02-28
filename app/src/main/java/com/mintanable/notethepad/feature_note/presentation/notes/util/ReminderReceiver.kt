package com.mintanable.notethepad.feature_note.presentation.notes.util

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.mintanable.notethepad.MainActivity
import com.mintanable.notethepad.R

class ReminderReceiver : BroadcastReceiver() {

    companion object{
        const val NOTE_ID = "NOTE_ID"
        const val NOTE_TITLE = "NOTE_TITLE"
        const val NOTE_CONTENT = "NOTE_CONTENT"
        const val CHANNEL_ID = "note_reminders"
    }
    @SuppressLint("ServiceCast")
    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getLongExtra(NOTE_ID, -1L)
        val noteTitle = intent.getStringExtra(NOTE_TITLE) ?: "Note Reminder"
        val noteContent = intent.getStringExtra(NOTE_CONTENT) ?: ""

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Note Reminders", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val clickIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("target_note_id", noteId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            noteId.toInt(),
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.notethepad_launcher_round)
            .setContentTitle(noteTitle)
            .setContentText(noteContent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(noteId.toInt(), notification)
    }

}