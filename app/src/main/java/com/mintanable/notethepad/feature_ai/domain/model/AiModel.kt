package com.mintanable.notethepad.feature_ai.domain.model

data class PromptTemplate(val title: String, val description: String, val prompt: String)

data class AiModel(
    /**
     * The name of the model.
     */
    val name: String,

    /**
     * The display name of the model, for display purpose.
     **/
    val displayName: String = "",

    /**
     * A description or information about the model (Markdown supported).
     */
    val info: String = "",


    /**
     * The task type ids that this model is best for.
     **/
    val bestForTaskIds: List<String> = listOf(),

    /**
     * The minimum device memory in GB to run the model.
     */
    val minDeviceMemoryInGb: Int? = null,

    /**
     * The URL to download the model from.
     */
    val url: String = "",

    /**
     * The size of the model file in bytes.
     **/
    val sizeInBytes: Long = 0L,

    /**
     * The name of the downloaded model file.
     * {context.getExternalFilesDir}/{normalizedName}/{version}/{downloadFileName}
     */
    val downloadFileName: String = "_",

    /***
     * The version of the model.
     */
    val version: String = "_",

    /** Whether the model is LLM or not. */
    val isLlm: Boolean = false,

    // End of model download related fields.
    //////////////////////////////////////////////////////////////////////////////////////////////////


    /** The prompt templates for the model */
    val llmPromptTemplates: List<PromptTemplate> = listOf(),

    /** Whether the LLM model supports image input. */
    val llmSupportImage: Boolean = false,

    /** Whether the LLM model supports audio input. */
    val llmSupportAudio: Boolean = false,

    /** The max token for llm model. */
    val llmMaxToken: Int = 0,

    /** Compatible accelerators. */
    val accelerators: List<Accelerator> = listOf(),

    /** Whether the model is imported or not. */
    val imported: Boolean = false,
)

enum class ModelDownloadStatusType {
    NOT_DOWNLOADED,
    PARTIALLY_DOWNLOADED,
    IN_PROGRESS,
    UNZIPPING,
    SUCCEEDED,
    FAILED,
}

data class ModelDownloadStatus(
    val status: ModelDownloadStatusType,
    val totalBytes: Long = 0,
    val receivedBytes: Long = 0,
    val errorMessage: String = "",
    val bytesPerSecond: Long = 0,
    val remainingMs: Long = 0,
)