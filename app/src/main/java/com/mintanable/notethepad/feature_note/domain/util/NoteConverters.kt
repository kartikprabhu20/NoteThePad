package com.mintanable.notethepad.feature_note.domain.util

import androidx.room.TypeConverter

class NoteConverters {
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return value.joinToString(separator = "|||")
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        if (value.isEmpty()) return emptyList()
        return value.split("|||")
    }
}