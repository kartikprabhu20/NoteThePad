package com.mintanable.notethepad.feature_settings.presentation.use_cases

data class PermissionUsecases(
    val markCameraPermissionFlag: MarkCameraPermissionFlag,
    val getCameraPermissionFlag: GetCameraPermissionFlag,
    val markMicrophonePermissionFlag: MarkMicrophonePermissionFlag,
    val getMicrophonePermissionFlag: GetMicrophonePermissionFlag,
)
