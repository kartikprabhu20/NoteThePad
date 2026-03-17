package com.mintanable.notethepad.core.model

import android.net.Uri

data class DetailedNote (
    val id: Long = 0L,
    val title: String,
    val content: String,
    val timestamp: Long,
    val color: Int,
    val imageUris: List<Uri> = emptyList(),
    val audioAttachments: List<Attachment> = emptyList(),
    val reminderTime: Long = -1,
    val checkListItems: List<CheckboxItem> = emptyList(),
    val isCheckboxListAvailable: Boolean = false,
    val tags: List<Tag> = emptyList()
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