package com.mintanable.notethepad.core.model.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PromptTemplate(val title: String, val description: String, val prompt: String)

@Serializable
data class AiModel(
    val name: String,
    val displayName: String = "",
    val info: String = "",
    val bestForTaskIds: List<String> = emptyList(),
    val minDeviceMemoryInGb: Int? = null,
    val url: String = "",
    val sizeInBytes: Long = 0L,
    val downloadFileName: String = "_",
    val version: String = "_",
    val isLlm: Boolean = false,
    val llmPromptTemplates: List<PromptTemplate> = emptyList(),
    val llmSupportImage: Boolean = false,
    val llmSupportAudio: Boolean = false,
    val llmMaxToken: Int = 0,
    val accelerators: List<Accelerator> = emptyList(),
    val imported: Boolean = false,
    val isDownloaded: Boolean = false,
)

@Serializable
enum class Accelerator(val label: String) {
    @SerialName("CPU") CPU("CPU"),
    @SerialName("GPU") GPU("GPU"),
    @SerialName("NPU") NPU("NPU"),
}

@Serializable
data class AiModelCatalog(
    val models: List<AiModelEntry>? = emptyList()
)

@Serializable
data class ModelDefaultConfig(
    val topK: Int? = 0,
    val topP: Float? = 0f,
    val temperature: Float? = 0f,
    val maxTokens: Int? = 1024,
    val accelerators: String? = ""
)

@Serializable
data class AiModelEntry(
    val name: String? = "",
    val modelId: String? = "",
    val modelFile: String? = "",
    val description: String? = "",
    val sizeInBytes: Long? = 0L,
    val minDeviceMemoryInGb: Int? = 0,
    val commitHash: String? = "",
    val llmSupportImage: Boolean? = false,
    val llmSupportAudio: Boolean? = false,
    val defaultConfig: ModelDefaultConfig? = null,
    val taskTypes: List<String>? = emptyList(),
    val bestForTaskTypes: List<String>? = emptyList()
) {
    fun toAiModel(): AiModel {
        val downloadUrl = "https://huggingface.co/${modelId ?: ""}/resolve/${commitHash ?: "main"}/${modelFile ?: ""}?download=true"

        val isLlmModel = taskTypes?.any {
            it in listOf("llm_chat", "llm_prompt_lab", "llm_ask_audio", "llm_ask_image")
        } ?: false

        var llmMaxToken = 1024
        var acceleratorsList: List<Accelerator> = emptyList()

        defaultConfig?.let { config ->
            llmMaxToken = config.maxTokens ?: 1024
            val configAccels = config.accelerators
            if (!configAccels.isNullOrEmpty()) {
                acceleratorsList = configAccels.split(",")
                    .mapNotNull { item ->
                        when (item.trim().lowercase()) {
                            "cpu" -> Accelerator.CPU
                            "gpu" -> Accelerator.GPU
                            "npu" -> Accelerator.NPU
                            else -> null
                        }
                    }
            }
        }

        return AiModel(
            name = name ?: "Unknown",
            displayName = name ?: "Unknown",
            version = commitHash ?: "",
            info = description ?: "",
            url = downloadUrl,
            sizeInBytes = sizeInBytes ?: 0L,
            minDeviceMemoryInGb = minDeviceMemoryInGb,
            downloadFileName = modelFile ?: "",
            llmSupportImage = llmSupportImage ?: false,
            llmSupportAudio = llmSupportAudio ?: false,
            llmMaxToken = llmMaxToken,
            accelerators = acceleratorsList,
            bestForTaskIds = bestForTaskTypes ?: emptyList(),
            isLlm = isLlmModel
        )
    }
}