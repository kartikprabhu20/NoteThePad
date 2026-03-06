package com.mintanable.notethepad.feature_note.domain.model

import android.net.Uri
import com.mintanable.notethepad.feature_note.domain.util.Attachment

data class DetailedNote (
    val id: Long? = null,
    val title: String,
    val content: String,
    val timestamp: Long,
    val color: Int,
    val imageUris: List<Uri> = emptyList(),
    val audioAttachments: List<Attachment> = emptyList(),
    val reminderTime: Long = -1,
    val checkListItems: List<CheckboxItem> = emptyList(),
    val isCheckboxListAvailable: Boolean = false
){
    fun toNote(): Note{
        return Note(
            id = id,
            title = title,
            content = content,
            timestamp = timestamp,
            color = color,
            imageUris = imageUris.map { it.toString() },
            audioUris = audioAttachments.map { it.uri.toString() },
            reminderTime = reminderTime,
        )
    }
}