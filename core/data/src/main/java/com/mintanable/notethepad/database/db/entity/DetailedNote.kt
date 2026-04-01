package com.mintanable.notethepad.database.db.entity

import com.mintanable.notethepad.core.model.note.CheckboxItem
import org.json.JSONObject

data class DetailedNote (
    val id: String = "",
    val title: String,
    val content: String,
    val timestamp: Long,
    val color: Int,
    val imageUris: List<String> = emptyList(),
    val audioAttachments: List<Attachment> = emptyList(),
    val reminderTime: Long = -1,
    val checkListItems: List<CheckboxItem> = emptyList(),
    val isCheckboxListAvailable: Boolean = false,
    val tagEntities: List<TagEntity> = emptyList(),
    val backgroundImage: Int = -1,
    val lastUpdateTime: Long = 0L
){
    fun toNote(): NoteEntity{
        val json = JSONObject()
        audioAttachments
            .filter { it.transcription.isNotEmpty() }
            .forEach { json.put(it.uri, it.transcription) }
        return NoteEntity(
            id = id,
            title = title,
            content = content,
            timestamp = timestamp,
            color = color,
            imageUris = imageUris,
            audioUris = audioAttachments.map { it.uri },
            reminderTime = reminderTime,
            audioTranscriptions = json.toString(),
            backgroundImage = backgroundImage
        )
    }
}