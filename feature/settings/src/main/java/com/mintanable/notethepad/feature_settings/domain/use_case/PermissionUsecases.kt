package com.mintanable.notethepad.feature_settings.domain.use_case

data class PermissionUsecases(
    val markCameraPermissionFlag: MarkCameraPermissionFlag,
    val getCameraPermissionFlag: GetCameraPermissionFlag,
    val markMicrophonePermissionFlag: MarkMicrophonePermissionFlag,
    val getMicrophonePermissionFlag: GetMicrophonePermissionFlag,
)
