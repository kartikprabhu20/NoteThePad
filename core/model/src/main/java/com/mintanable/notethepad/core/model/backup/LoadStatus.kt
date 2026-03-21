package com.mintanable.notethepad.core.model.backup

sealed class LoadStatus {
    object Idle : LoadStatus()
    data class Progress(val percentage: Int, val type: LoadType, val bytes: Long = 0, val totalBytes: Long = 0) : LoadStatus()
    object Success : LoadStatus()
    data class Error(val message: String) : LoadStatus()
}

enum class LoadType(name: String){
    UPLOAD("Upload"),
    DOWNLOAD("Download")
}
