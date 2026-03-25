package com.mintanable.notethepad.feature_note.domain.use_case.notes

import android.net.Uri
import android.util.Log
import com.mintanable.notethepad.core.common.CheckboxConvertors
import com.mintanable.notethepad.file.FileManager
import com.mintanable.notethepad.core.model.note.CheckboxItem
import com.mintanable.notethepad.database.db.entity.InvalidNoteException
import com.mintanable.notethepad.database.db.entity.NoteEntity
import com.mintanable.notethepad.database.db.entity.TagEntity
import com.mintanable.notethepad.database.db.repository.NoteRepository
import org.json.JSONObject

class SaveNoteWithAttachments(
    private val repository: NoteRepository,
    private val fileManager: FileManager
) {

    @Throws(InvalidNoteException::class)
    suspend operator fun invoke(noteEntity: NoteEntity, tagEntities: List<TagEntity> = emptyList()) : Long{
        if(noteEntity.title.isBlank()){
            throw InvalidNoteException("The title of the note cant be empty")
        }
        return repository.insertNote(noteEntity, tagEntities)
    }

    suspend operator fun invoke(
        id: Long,
        title: String,
        content: String,
        timestamp: Long,
        color: Int,
        imageUris: List<Uri> = emptyList(),
        audioUris: List<Uri> = emptyList(),
        audioTranscriptions: List<String> = emptyList(),
        reminderTime: Long,
        checkboxItems: List<CheckboxItem>,
        tagEntities: List<TagEntity> = emptyList()
    ) : Result<Long> {

        Log.d("kptest", "SaveNoteWithAttachments invoke: ${imageUris+audioUris}")
       try{
            if(title.isBlank()){
                return Result.failure(InvalidNoteException("The title of the note cant be empty"))
            }

           val newContent = if(checkboxItems.isNotEmpty()) CheckboxConvertors.checkboxesToString(checkboxItems) else content

            val imageUriList = imageUris.mapNotNull { uri ->
                fileManager.saveMediaToStorage(uri, fileManager.getAttachmentType(uri).name.lowercase())
            }

            val audioUriList = audioUris.mapNotNull { uri ->
                fileManager.saveMediaToStorage(uri, fileManager.getAttachmentType(uri).name.lowercase())
            }

           Log.d("kptest", "SaveNoteWithAttachments save: ${imageUriList+audioUriList}")
           val transcriptionsJson = run {
               val json = JSONObject()
               audioUriList.zip(audioTranscriptions).forEach { (savedUri, transcript) ->
                   if (transcript.isNotEmpty()) json.put(savedUri, transcript)
               }
               json.toString()
           }
           val newNoteEntityId = repository.insertNote(
                NoteEntity(
                    id = id,
                    title = title,
                    content = newContent,
                    timestamp = timestamp,
                    color = color,
                    imageUris = imageUriList,
                    audioUris = audioUriList,
                    reminderTime = reminderTime,
                    audioTranscriptions = transcriptionsJson
                ),
                tagEntities
            )
            return Result.success(newNoteEntityId)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}