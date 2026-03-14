package com.mintanable.notethepad.feature_backup.presentation

sealed class LoadStatus {
    object Idle : LoadStatus()
    data class Progress(val percentage: Int, val type: LoadType) : LoadStatus()
    object Success : LoadStatus()
    data class Error(val message: String) : LoadStatus()
}

enum class LoadType(name: String){
    UPLOAD("Upload"),
    DOWNLOAD("Download")
}
