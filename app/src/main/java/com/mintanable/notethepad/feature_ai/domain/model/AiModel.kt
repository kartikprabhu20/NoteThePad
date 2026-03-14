package com.mintanable.notethepad.feature_ai.domain.model

enum class AiModelType(
    val displayName: String,
    val storageSize: String,
    val description: String,
    val fileName: String?,
    val modelUrl: String? = null
) {
    NONE(
        "None",
        "0 MB",
        "No AI assistance will be provided.",
        null
    ),
    CLOUD_FLASH(
        "Gemini 3 Flash (Cloud)",
        "0 MB",
        "Fastest response, requires internet connection. Ideal for general note assistance.",
        null
    ),
    SYSTEM_NANO(
        "Gemini Nano (System)",
        "Pre-installed",
        "On-device privacy and performance. Only available on supported Pixel/Samsung devices.",
        null
    ),
    GEMMA_1B(
        "Gemma 3 1B",
        "~585 MB",
        "Lightweight on-device model. Good balance between speed and performance.",
        "gemma3_1b.task",
        "https://huggingface.co/google/gemma-3-1b-it-android/resolve/main/gemma3-1b-it-int4.task"
    ),
    GEMMA_E2B(
        "Gemma 3 E2B",
        "~3.7 GB",
        "Higher quality on-device reasoning. Requires more storage and RAM.",
        "e2b_it.task",
        "https://huggingface.co/google/gemma-3-4b-it-android/resolve/main/gemma-3-4b-it-int4.task"
    ),
    GEMMA_E4B(
        "Gemma 3 E4B",
        "~4.9 GB",
        "Best on-device performance. Recommended for powerful devices only.",
        "e4b_it.task",
        "https://huggingface.co/google/gemma-3-12b-it-android/resolve/main/gemma-3-12b-it-int8.task"
    )
}

sealed class ModelStatus {
    object NotDownloaded : ModelStatus()
    data class Downloading(val progress: Int) : ModelStatus()
    object Ready : ModelStatus()
}
