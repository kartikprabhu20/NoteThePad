package com.mintanable.notethepad.feature_note.presentation

import android.net.Uri
import androidx.compose.runtime.Stable
import com.mintanable.notethepad.core.richtext.model.RichTextState
import com.mintanable.notethepad.database.db.entity.Attachment
import com.mintanable.notethepad.core.model.note.CheckboxItem
import com.mintanable.notethepad.core.model.note.MediaState
import com.mintanable.notethepad.core.model.collaboration.Collaborator
import com.mintanable.notethepad.database.db.entity.TagEntity
import com.mintanable.notethepad.feature_note.presentation.notes.BottomSheetType
import com.mintanable.notethepad.permissions.DeniedType

@Stable
data class AddEditNoteUiState(
    val titleState: NoteTextFieldState = NoteTextFieldState(hint = "Enter title..."),

    val contentState: NoteTextFieldState = NoteTextFieldState(hint = "Enter some content..."),
    val contentRichTextState: RichTextState = RichTextState.EMPTY,
    val isRichTextBarActive: Boolean = false,

    val noteColor: Int = -1,
    val backgroundImage: Int = -1,
    val attachedImages: List<Uri> = emptyList(),
    val attachedPaints: List<Uri> = emptyList(),
    val attachedAudios: List<Attachment> = emptyList(),
    val reminderTime: Long = -1L,
    val checkListItems: List<CheckboxItem> = emptyList(),
    val isCheckboxListAvailable: Boolean = false,
    val tagEntities: List<TagEntity> = emptyList(),
    val liveTranscription: String = "",

    val isRecording: Boolean = false,
    val recordingAmplitude: Int = 0,
    val transcribingUri: String? = null,
    val isSaving: Boolean = false,
    val currentSheetType: BottomSheetType = BottomSheetType.NONE,
    val settingsDeniedType: DeniedType? = null,
    val showCameraRationale: Boolean = false,
    val showMicrophoneRationale: Boolean = false,
    val showAlarmPermissionRationale: Boolean = false,
    val zoomedImageUri: Uri? = null,
    val zoomedImageIndex: Int = 0,
    val mediaState: MediaState? = null,
    val showDataAndTimePicker: Boolean = false,
    val showAddNewTagDialog: Boolean = false,

    val isTagSuggestionLoading: Boolean = false,
    val suggestedTags: List<String> = emptyList(),

    val imageSuggestions: List<String> = emptyList(),
    val isAnalyzingImage: Boolean = false,
    val imageQueryResult: String = "",
    val isImageQueryLoading: Boolean = false,
    val showStopAIConfirmation: Boolean = false,

    val collaborators: List<Collaborator> = emptyList(),
    val isLoadingCollaborators: Boolean = false,
    val collaboratorError: String? = null,
    val showLoginAndEnableCloudSyncDialog: Boolean = false,
    val isOwner: Boolean = true,
    val summary: String = "",
    val showDeleteConfirmation: Boolean = false
) {
    fun toNoteDataState(): NoteDataState {
        return NoteDataState(
            title = this.titleState.richText.rawText,
            content = this.contentRichTextState.document.rawText,
            noteColor = this.noteColor,
            backgroundImage = this.backgroundImage,
            attachedImages = this.attachedImages.toList(),
            attachedPaints = this.attachedPaints.toList(),
            attachedAudios = this.attachedAudios.toList(),
            reminderTime = this.reminderTime,
            checkListItems = this.checkListItems.toList(),
            isCheckboxListAvailable = this.isCheckboxListAvailable,
            tagEntities = this.tagEntities.toList(),
            summary = this.summary
        )
    }

    fun toEditNoteSnapshot(): EditNoteSnapshot{
        return EditNoteSnapshot(
            titleDocument = this.titleState.richText,
            contentDocument = this.contentRichTextState.document,
            noteColor = this.noteColor,
            backgroundImage = this.backgroundImage,
            reminderTime = this.reminderTime,
            checkListItems = this.checkListItems.toList(),
            isCheckboxListAvailable = this.isCheckboxListAvailable,
            tagEntities = this.tagEntities.toList()
        )
    }
}
