package com.mintanable.notethepad.feature_note.domain.util

import androidx.collection.LruCache
import androidx.core.net.toUri
import com.mintanable.notethepad.feature_note.data.repository.AudioMetadataProvider
import com.mintanable.notethepad.feature_note.domain.model.DetailedNote
import com.mintanable.notethepad.feature_note.domain.model.Note
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class DetailedNoteMapper @Inject constructor(
    private val audioMetadataProvider: AudioMetadataProvider
) {

    private val durationCache = LruCache<String, Long>(100)

    suspend fun toDetailedNote(note: Note): DetailedNote = coroutineScope {

        val audioAttachments = if (note.audioUris.isEmpty()) {
            emptyList()
        } else {
            note.audioUris.map { uriString ->
                async {
                    val uri = uriString.toUri()
                    var duration = durationCache[uriString]
                    if (duration == null) {
                        duration = audioMetadataProvider.getDuration(uri)
                        durationCache.put(uriString, duration)
                    }
                    Attachment(uri, duration)
                }
            }.awaitAll()
        }

        val isCheckbox = CheckboxConvertors.isContentCheckboxList(note.content)

        DetailedNote(
            id = note.id,
            title = note.title,
            content = note.content,
            timestamp = note.timestamp,
            color = note.color,
            imageUris = note.imageUris.map { it.toUri() },
            audioAttachments = audioAttachments,
            reminderTime = note.reminderTime,
            checkListItems = if (isCheckbox) CheckboxConvertors.stringToCheckboxes(note.content) else emptyList(),
            isCheckboxListAvailable = isCheckbox
        )
    }
}