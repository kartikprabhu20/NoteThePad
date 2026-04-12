package com.mintanable.notethepad.database.db.util

import com.mintanable.notethepad.core.network.sync.NoteDto
import com.mintanable.notethepad.core.network.sync.NoteTagCrossRefDto
import com.mintanable.notethepad.core.network.sync.TagDto
import com.mintanable.notethepad.database.db.entity.NoteEntity
import com.mintanable.notethepad.database.db.entity.NoteTagCrossRef
import com.mintanable.notethepad.database.db.entity.TagEntity
import org.json.JSONObject
import java.io.File

fun NoteEntity.toDto(): NoteDto {
    val portableImageUris = imageUris.map { File(it).name }
    val portableAudioUris = audioUris.map { File(it).name }
    val portableTranscriptions = remapTranscriptionKeys(audioTranscriptions) { File(it).name }
    return NoteDto(
        id, title, content, timestamp, color, portableImageUris, portableAudioUris,
        reminderTime, portableTranscriptions, backgroundImage, lastUpdateTime, userId, isDeleted
    )
}

fun TagEntity.toDto() = TagDto(tagName, tagId, lastUpdateTime, userId, isDeleted)
fun NoteTagCrossRef.toDto() = NoteTagCrossRefDto(noteId, tagId, userId, isDeleted, lastUpdateTime)

fun NoteDto.toEntity(mediaDir: File): NoteEntity {
    val resolvedImageUris = imageUris.map { resolveMediaPath(it, mediaDir) }
    val resolvedAudioUris = audioUris.map { resolveMediaPath(it, mediaDir) }
    val resolvedTranscriptions = remapTranscriptionKeys(audioTranscriptions) { resolveMediaPath(it, mediaDir) }
    return NoteEntity(
        id = id, title = title, content = content, timestamp = timestamp, color = color,
        imageUris = resolvedImageUris, audioUris = resolvedAudioUris, reminderTime = reminderTime,
        audioTranscriptions = resolvedTranscriptions, backgroundImage = backgroundImage,
        lastUpdateTime = lastUpdateTime, userId = userId, isDeleted = isDeleted,
        isSynced = true
    )
}

fun TagDto.toEntity() = TagEntity(
    tagName = tagName, tagId = tagId, lastUpdateTime = lastUpdateTime, userId = userId, isDeleted = isDeleted
)

fun NoteTagCrossRefDto.toEntity() = NoteTagCrossRef(
    noteId = noteId, tagId = tagId, userId = userId, isDeleted = isDeleted, lastUpdateTime = lastUpdateTime
)

private fun resolveMediaPath(path: String, mediaDir: File): String {
    val file = File(path)
    if (file.isAbsolute && file.exists()) return path
    val fileName = file.name
    return File(mediaDir, fileName).absolutePath
}

private fun remapTranscriptionKeys(json: String, transform: (String) -> String): String {
    if (json.isBlank()) return json
    return try {
        val old = JSONObject(json)
        val result = JSONObject()
        old.keys().forEach { key -> result.put(transform(key), old.getString(key)) }
        result.toString()
    } catch (e: Exception) { json }
}