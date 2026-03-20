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
    val bestForTaskIds: List<String> = emptyList(), // Use emptyList() over listOf() for slightly better performance
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
)

@Serializable
enum class Accelerator(val label: String) {
    @SerialName("CPU") CPU("CPU"),
    @SerialName("GPU") GPU("GPU"),
    @SerialName("NPU") NPU("NPU"),
}

@Serializable
data class AiModelCatalog(
    val models: List<AiModelEntry> // No @SerialName needed if the JSON key matches the variable name
)

@Serializable
data class ModelDefaultConfig(
    val topK: Int,
    val topP: Float,
    val temperature: Float,
    val maxTokens: Int,
    val accelerators: String
)

@Serializable
data class AiModelEntry(
    val name: String,
    val modelId: String,
    val modelFile: String,
    val description: String,
    val sizeInBytes: Long,
    val minDeviceMemoryInGb: Int,
    val commitHash: String,
    val llmSupportImage: Boolean = false,
    val llmSupportAudio: Boolean = false,
    val defaultConfig: ModelDefaultConfig,
    val taskTypes: List<String>,
    val bestForTaskTypes: List<String> = emptyList()
) {
    fun toAiModel(): AiModel {
        val downloadUrl = "https://huggingface.co/$modelId/resolve/$commitHash/$modelFile?download=true"

        val isLlmModel = taskTypes.any {
            it in listOf("llm_chat", "llm_prompt_lab", "llm_ask_audio", "llm_ask_image")
        }

        var llmMaxToken = 1024
        var acceleratorsList: List<Accelerator> = emptyList()

        // defaultConfig is non-nullable in @Serializable definition above,
        // but if the JSON might lack it, make it nullable in the data class: ModelDefaultConfig?
        llmMaxToken = defaultConfig.maxTokens
        val configAccels = defaultConfig.accelerators
        if (configAccels.isNotEmpty()) {
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

        return AiModel(
            name = name,
            displayName = name,
            version = commitHash,
            info = description,
            url = downloadUrl,
            sizeInBytes = sizeInBytes,
            minDeviceMemoryInGb = minDeviceMemoryInGb,
            downloadFileName = modelFile,
            llmSupportImage = llmSupportImage,
            llmSupportAudio = llmSupportAudio,
            llmMaxToken = llmMaxToken,
            accelerators = acceleratorsList,
            bestForTaskIds = bestForTaskTypes,
            isLlm = isLlmModel
        )
    }
}