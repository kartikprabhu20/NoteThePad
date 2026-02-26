package com.mintanable.notethepad.feature_note.domain

import android.net.Uri
import androidx.compose.ui.graphics.toArgb
import com.mintanable.notethepad.feature_note.domain.model.NoteColors
import com.mintanable.notethepad.feature_note.presentation.notes.BottomSheetType
import com.mintanable.notethepad.feature_note.presentation.notes.NoteTextFieldState
import com.mintanable.notethepad.feature_settings.presentation.util.DeniedType

data class AddEditNoteUiState(
    val titleState: NoteTextFieldState = NoteTextFieldState(hint = "Enter title..."),
    val contentState: NoteTextFieldState = NoteTextFieldState(hint = "Enter some content..."),
    val noteColor: Int = NoteColors.colors.random().toArgb(),
    val attachedImages: List<Uri> = emptyList(),
    val attachedAudios: List<Uri> = emptyList(),

    val isRecording: Boolean = false,
    val isSaving: Boolean = false,

    val currentSheetType: BottomSheetType = BottomSheetType.NONE,
    val settingsDeniedType: DeniedType? = null,
    val showCameraRationale: Boolean = false,
    val showMicrophoneRationale: Boolean = false,
    val zoomedImageUri: Uri? = null,
    val nowPlayingAudioUri: Uri? = null
)