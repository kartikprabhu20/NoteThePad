package com.mintanable.notethepad.feature_note.domain.use_case.notes

import android.content.Context
import android.net.Uri
import com.mintanable.notethepad.core.common.AttachmentHelper
import com.mintanable.notethepad.core.common.CheckboxConvertors
import com.mintanable.notethepad.file.FileManager
import com.mintanable.notethepad.core.model.note.CheckboxItem
import com.mintanable.notethepad.core.model.note.InvalidNoteException
import com.mintanable.notethepad.core.model.note.NoteEntity
import com.mintanable.notethepad.core.model.note.TagEntity
import com.mintanable.notethepad.database.db.repository.NoteRepository
import dagger.hilt.android.qualifiers.ApplicationContext

class SaveNoteWithAttachments(
    private val repository: NoteRepository,
    private val fileManager: FileManager,
    @ApplicationContext val context: Context
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
        reminderTime: Long,
        checkboxItems: List<CheckboxItem>,
        tagEntities: List<TagEntity> = emptyList()
    ) : Result<Long> {

       try{
            if(title.isBlank()){
                return Result.failure(InvalidNoteException("The title of the note cant be empty"))
            }

           val newContent = if(checkboxItems.isNotEmpty()) CheckboxConvertors.checkboxesToString(checkboxItems) else content

            val imageUriList = imageUris.mapNotNull { uri ->
                if (uri.toString().contains(context.packageName)) {
                    uri.toString()
                } else {
                    fileManager.saveMediaToStorage(uri, AttachmentHelper.getAttachmentType(context, uri).name.lowercase())
                }
            }

            val audioUriList = audioUris.mapNotNull { uri ->
                if (uri.toString().contains(context.packageName)) {
                    uri.toString()
                } else {
                    fileManager.saveMediaToStorage(uri, AttachmentHelper.getAttachmentType(context, uri).name.lowercase())
                }
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
                    reminderTime = reminderTime
                ),
                tagEntities
            )
            return Result.success(newNoteEntityId)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}
