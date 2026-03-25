package com.mintanable.notethepad.database.helper

import androidx.collection.LruCache
import androidx.core.net.toUri
import com.mintanable.notethepad.core.common.CheckboxConvertors
import com.mintanable.notethepad.core.common.DispatcherProvider
import com.mintanable.notethepad.database.db.entity.Attachment
import com.mintanable.notethepad.database.db.entity.DetailedNote
import com.mintanable.notethepad.database.db.entity.NoteEntity
import com.mintanable.notethepad.database.db.entity.TagEntity
import com.mintanable.notethepad.database.db.util.AudioMetadataProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject

class DetailedNoteMapper @Inject constructor(
    private val audioMetadataProvider: AudioMetadataProvider,
    private val dispatchers: DispatcherProvider
) {

    private val durationCache = LruCache<String, Long>(100)

    suspend fun toDetailedNote(noteEntity: NoteEntity, tagEntities: List<TagEntity> = emptyList()): DetailedNote = withContext(dispatchers.default) {

        val transcriptionMap: Map<String, String> = runCatching {
            val json = JSONObject(noteEntity.audioTranscriptions)
            json.keys().asSequence().associateWith { json.getString(it) }
        }.getOrDefault(emptyMap())

        val audioAttachments = if (noteEntity.audioUris.isEmpty()) {
            emptyList()
        } else {
            noteEntity.audioUris.map { uriString ->
                async {
                    var duration = durationCache[uriString]
                    if (duration == null) {
                        duration = audioMetadataProvider.getDuration(uriString.toUri())
                        durationCache.put(uriString, duration)
                    }
                    Attachment(uriString, duration, transcriptionMap[uriString] ?: "")
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
            imageUris = noteEntity.imageUris,
            audioAttachments = audioAttachments,
            reminderTime = noteEntity.reminderTime,
            checkListItems = if (isCheckbox) CheckboxConvertors.stringToCheckboxes(noteEntity.content) else emptyList(),
            isCheckboxListAvailable = isCheckbox,
            tagEntities = tagEntities
        )
    }
}
