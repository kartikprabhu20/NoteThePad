package com.mintanable.notethepad.feature_note.domain

import android.net.Uri
import com.mintanable.notethepad.feature_note.domain.model.CheckboxItem
import com.mintanable.notethepad.feature_note.domain.util.Attachment
import com.mintanable.notethepad.feature_note.domain.util.MediaState
import com.mintanable.notethepad.feature_note.presentation.notes.BottomSheetType
import com.mintanable.notethepad.feature_note.presentation.notes.NoteTextFieldState
import com.mintanable.notethepad.feature_settings.presentation.util.DeniedType

data class AddEditNoteUiState(
    val titleState: NoteTextFieldState = NoteTextFieldState(hint = "Enter title..."),
    val contentState: NoteTextFieldState = NoteTextFieldState(hint = "Enter some content..."),
    val noteColor: Int = -1,
    val attachedImages: List<Uri> = emptyList(),
    val attachedAudios: List<Attachment> = emptyList(),
    val reminderTime: Long = -1L,
    val checkListItems: List<CheckboxItem> = emptyList(),

    val isRecording: Boolean = false,
    val isSaving: Boolean = false,
    val isCheckboxListAvailable: Boolean = false,

    val currentSheetType: BottomSheetType = BottomSheetType.NONE,
    val settingsDeniedType: DeniedType? = null,
    val showCameraRationale: Boolean = false,
    val showMicrophoneRationale: Boolean = false,
    val showAlarmPermissionRationale: Boolean = false,
    val zoomedImageUri: Uri? = null,
    val mediaState: MediaState? = null,
    val showDataAndTimePicker: Boolean = false
)