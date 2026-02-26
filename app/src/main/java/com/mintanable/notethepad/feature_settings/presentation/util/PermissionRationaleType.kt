package com.mintanable.notethepad.feature_settings.presentation.util

enum class PermissionRationaleType(val title: String,val message: String) {
    NOTIFICATION("Notification permission","We use notifications to show you the upload progress. Without this, you won't know if the backup finishes successfully while the app is in the background."),
    CAMERA("Camera permission", "We use camera to add images and video to your note item. Without this you wont be able to attach images/videos directly, instead attach it from gallery"),
    MICROPHONE("Microphone permission", "We use microphone to record and attach voice notes. Without this you will not be able to create voice notes."),
    CAMERA_DENIED("Camera Permission Required", "You have permanently denied camera access. To take photos for your notes, please enable it in the app settings."),
    MICROPHONE_DENIED("Microphone Permission Required", "You have permanently denied microphone access. To record voice notes, please enable it in the app settings."),
}

enum class DeniedType { CAMERA, MICROPHONE }