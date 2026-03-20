package com.mintanable.notethepad.feature_note.domain.use_case.permissions

data class PermissionUsecases(
    val markCameraPermissionFlag: MarkCameraPermissionFlag,
    val getCameraPermissionFlag: GetCameraPermissionFlag,
    val markMicrophonePermissionFlag: MarkMicrophonePermissionFlag,
    val getMicrophonePermissionFlag: GetMicrophonePermissionFlag,
)