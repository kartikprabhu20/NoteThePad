package com.mintanable.notethepad.core.network.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NoteDto(
    val id: String,
    val title: String,
    val content: String,
    val timestamp: Long,
    val color: Int,
    @SerialName("image_uris")
    val imageUris: List<String>,
    @SerialName("audio_uris")
    val audioUris: List<String>,
    @SerialName("reminder_time")
    val reminderTime: Long,
    @SerialName("audio_transcriptions")
    val audioTranscriptions: String,
    @SerialName("background_image")
    val backgroundImage: Int,
    @SerialName("last_update_time")
    val lastUpdateTime: Long,
    @SerialName("user_id")
    val userId: String?,
    @SerialName("is_deleted")
    val isDeleted: Boolean
)

@Serializable
data class TagDto(
    @SerialName("tag_name")
    val tagName: String,
    @SerialName("tag_id")
    val tagId: String,
    @SerialName("last_update_time")
    val lastUpdateTime: Long,
    @SerialName("user_id")
    val userId: String?,
    @SerialName("is_deleted")
    val isDeleted: Boolean
)

@Serializable
data class NoteTagCrossRefDto(
    @SerialName("note_id")
    val noteId: String,
    @SerialName("tag_id")
    val tagId: String,
    @SerialName("user_id")
    val userId: String?,
    @SerialName("is_deleted")
    val isDeleted: Boolean,
    @SerialName("last_update_time")
    val lastUpdateTime: Long
)
