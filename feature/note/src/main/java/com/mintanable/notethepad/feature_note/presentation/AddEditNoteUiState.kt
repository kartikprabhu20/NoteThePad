package com.mintanable.notethepad.feature_note.presentation

import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.input.TextFieldValue
import com.mintanable.notethepad.core.richtext.model.SpanType
import com.mintanable.notethepad.database.db.entity.Attachment
import com.mintanable.notethepad.core.model.note.CheckboxItem
import com.mintanable.notethepad.core.model.note.MediaState
import com.mintanable.notethepad.database.db.entity.TagEntity
import com.mintanable.notethepad.feature_note.presentation.notes.BottomSheetType
import com.mintanable.notethepad.permissions.DeniedType

@Stable
data class AddEditNoteUiState(
    val titleState: NoteTextFieldState = NoteTextFieldState(hint = "Enter title..."),
    val contentState: NoteTextFieldState = NoteTextFieldState(hint = "Enter some content..."),
    val contentTextFieldValue: TextFieldValue = TextFieldValue(),
    val isRichTextBarActive: Boolean = false,
    val activeContentStyles: Set<SpanType> = emptySet(),
    val noteColor: Int = -1,
    val attachedImages: List<Uri> = emptyList(),
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
    val mediaState: MediaState? = null,
    val showDataAndTimePicker: Boolean = false,
    val showAddNewTagDialog: Boolean = false,

    val isTagSuggestionLoading: Boolean = false,
    val suggestedTags: List<String> = emptyList(),

    val imageSuggestions: List<String> = emptyList(),
    val isAnalyzingImage: Boolean = false,
    val imageQueryResult: String = "",
    val isImageQueryLoading: Boolean = false
)