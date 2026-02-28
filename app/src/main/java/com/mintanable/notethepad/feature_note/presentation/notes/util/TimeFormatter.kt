package com.mintanable.notethepad.feature_note.presentation.notes.util

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object TimeFormatter {

    @RequiresApi(Build.VERSION_CODES.O)
    fun formatMillis(millis: Long): String {
        if (millis == -1L) return "No Reminder"

        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
        val instant = Instant.ofEpochMilli(millis)
        val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())

        return dateTime.format(formatter)
    }
}