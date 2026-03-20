package com.mintanable.notethepad.core.model.ai

sealed class AiModelDownloadStatus {
    object Unavailable : AiModelDownloadStatus()
    object Ready : AiModelDownloadStatus()
    object Downloading : AiModelDownloadStatus()
    object Downloadable : AiModelDownloadStatus()
}