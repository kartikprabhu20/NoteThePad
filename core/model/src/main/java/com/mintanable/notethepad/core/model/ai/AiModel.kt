package com.mintanable.notethepad.core.model.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PromptTemplate(
    @SerialName("title") val title: String,
    @SerialName("description") val description: String,
    @SerialName("prompt") val prompt: String
)

@Serializable
data class AiModel(
    @SerialName("name") val name: String,
    @SerialName("displayName") val displayName: String = "",
    @SerialName("info") val info: String = "",
    @SerialName("bestForTaskIds") val bestForTaskIds: List<String> = emptyList(),
    @SerialName("minDeviceMemoryInGb") val minDeviceMemoryInGb: Int? = null,
    @SerialName("url") val url: String = "",
    @SerialName("sizeInBytes") val sizeInBytes: Long = 0L,
    @SerialName("downloadFileName") val downloadFileName: String = "_",
    @SerialName("version") val version: String = "_",
    @SerialName("isLlm") val isLlm: Boolean = false,
    @SerialName("llmPromptTemplates") val llmPromptTemplates: List<PromptTemplate> = emptyList(),
    @SerialName("llmSupportImage") val llmSupportImage: Boolean = false,
    @SerialName("llmSupportAudio") val llmSupportAudio: Boolean = false,
    @SerialName("llmMaxToken") val llmMaxToken: Int = 0,
    @SerialName("accelerators") val accelerators: List<Accelerator> = emptyList(),
    @SerialName("imported") val imported: Boolean = false,
    @SerialName("isDownloaded") val isDownloaded: Boolean = false,
)

@Serializable
enum class Accelerator(val label: String) {
    @SerialName("CPU") CPU("CPU"),
    @SerialName("GPU") GPU("GPU"),
    @SerialName("NPU") NPU("NPU"),
}

@Serializable
data class AiModelCatalog(
    @SerialName("models") val models: List<AiModelEntry>? = emptyList()
)

@Serializable
data class ModelDefaultConfig(
    @SerialName("topK") val topK: Int? = 0,
    @SerialName("topP") val topP: Float? = 0f,
    @SerialName("temperature") val temperature: Float? = 0f,
    @SerialName("maxTokens") val maxTokens: Int? = 1024,
    @SerialName("accelerators") val accelerators: String? = ""
)

@Serializable
data class AiModelEntry(
    @SerialName("name") val name: String? = "",
    @SerialName("modelId") val modelId: String? = "",
    @SerialName("modelFile") val modelFile: String? = "",
    @SerialName("description") val description: String? = "",
    @SerialName("sizeInBytes") val sizeInBytes: Long? = 0L,
    @SerialName("minDeviceMemoryInGb") val minDeviceMemoryInGb: Int? = 0,
    @SerialName("commitHash") val commitHash: String? = "",
    @SerialName("llmSupportImage") val llmSupportImage: Boolean? = false,
    @SerialName("llmSupportAudio") val llmSupportAudio: Boolean? = false,
    @SerialName("defaultConfig") val defaultConfig: ModelDefaultConfig? = null,
    @SerialName("taskTypes") val taskTypes: List<String>? = emptyList(),
    @SerialName("bestForTaskTypes") val bestForTaskTypes: List<String>? = emptyList()
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