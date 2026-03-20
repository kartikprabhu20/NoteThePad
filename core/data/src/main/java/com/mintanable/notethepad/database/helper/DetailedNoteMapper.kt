package com.mintanable.notethepad.database.helper

import androidx.collection.LruCache
import androidx.core.net.toUri
import com.mintanable.notethepad.core.common.CheckboxConvertors
import com.mintanable.notethepad.core.common.DispatcherProvider
import com.mintanable.notethepad.core.model.note.Attachment
import com.mintanable.notethepad.core.model.note.DetailedNote
import com.mintanable.notethepad.core.model.note.NoteEntity
import com.mintanable.notethepad.core.model.note.TagEntity
import com.mintanable.notethepad.database.db.util.AudioMetadataProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DetailedNoteMapper @Inject constructor(
    private val audioMetadataProvider: AudioMetadataProvider,
    private val dispatchers: DispatcherProvider
) {

    private val durationCache = LruCache<String, Long>(100)

    suspend fun toDetailedNote(noteEntity: NoteEntity, tagEntities: List<TagEntity> = emptyList()): DetailedNote = withContext(dispatchers.default) {

        val audioAttachments = if (noteEntity.audioUris.isEmpty()) {
            emptyList()
        } else {
            noteEntity.audioUris.map { uriString ->
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

        val isCheckbox = CheckboxConvertors.isContentCheckboxList(noteEntity.content)

        DetailedNote(
            id = noteEntity.id,
            title = noteEntity.title,
            content = noteEntity.content,
            timestamp = noteEntity.timestamp,
            color = noteEntity.color,
            imageUris = noteEntity.imageUris.map { it.toUri() },
            audioAttachments = audioAttachments,
            reminderTime = noteEntity.reminderTime,
            checkListItems = if (isCheckbox) CheckboxConvertors.stringToCheckboxes(noteEntity.content) else emptyList(),
            isCheckboxListAvailable = isCheckbox,
            tagEntities = tagEntities
        )
    }
}
