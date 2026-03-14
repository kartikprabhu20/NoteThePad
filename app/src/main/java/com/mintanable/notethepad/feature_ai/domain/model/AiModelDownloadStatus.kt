package com.mintanable.notethepad.feature_ai.domain.model

sealed class AiModelDownloadStatus {
    object Unavailable : AiModelDownloadStatus()
    object Ready : AiModelDownloadStatus()
    object Downloading : AiModelDownloadStatus()
    object Downloadable : AiModelDownloadStatus()
}