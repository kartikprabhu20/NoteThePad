package com.mintanable.notethepad.core.model

import com.google.gson.annotations.SerializedName

data class PromptTemplate(val title: String, val description: String, val prompt: String)

data class AiModel(
    val name: String,
    val displayName: String = "",
    val info: String = "",
    val bestForTaskIds: List<String> = listOf(),
    val minDeviceMemoryInGb: Int? = null,
    val url: String = "",
    val sizeInBytes: Long = 0L,
    val downloadFileName: String = "_",
    val version: String = "_",
    val isLlm: Boolean = false,
    val llmPromptTemplates: List<PromptTemplate> = listOf(),
    val llmSupportImage: Boolean = false,
    val llmSupportAudio: Boolean = false,
    val llmMaxToken: Int = 0,
    val accelerators: List<Accelerator> = listOf(),
    val imported: Boolean = false,
)

enum class Accelerator(val label: String) {
    CPU(label = "CPU"),
    GPU(label = "GPU"),
    NPU(label = "NPU"),
}

data class AiModelCatalog(
    @SerializedName("models") val models: List<AiModelEntry>
)

data class ModelDefaultConfig(
    @SerializedName("topK") val topK: Int,
    @SerializedName("topP") val topP: Float,
    @SerializedName("temperature") val temperature: Float,
    @SerializedName("maxTokens") val maxTokens: Int,
    @SerializedName("accelerators") val accelerators: String
)

data class AiModelEntry(
    @SerializedName("name") val name: String,
    @SerializedName("modelId") val modelId: String,
    @SerializedName("modelFile") val modelFile: String,
    @SerializedName("description") val description: String,
    @SerializedName("sizeInBytes") val sizeInBytes: Long,
    @SerializedName("minDeviceMemoryInGb") val minDeviceMemoryInGb: Int,
    @SerializedName("commitHash") val commitHash: String,
    @SerializedName("llmSupportImage") val llmSupportImage: Boolean = false,
    @SerializedName("llmSupportAudio") val llmSupportAudio: Boolean = false,
    @SerializedName("defaultConfig") val defaultConfig: ModelDefaultConfig,
    @SerializedName("taskTypes") val taskTypes: List<String>,
    @SerializedName("bestForTaskTypes") val bestForTaskTypes: List<String> = emptyList()
) {
    fun toAiModel(): AiModel {
        val version = commitHash
        val downloadedFileName = modelFile
        val downloadUrl = "https://huggingface.co/$modelId/resolve/$commitHash/$modelFile?download=true"
        
        val isLlmModel = taskTypes.contains("llm_chat") || 
                         taskTypes.contains("llm_prompt_lab") || 
                         taskTypes.contains("llm_ask_audio") || 
                         taskTypes.contains("llm_ask_image")
        
        var llmMaxToken = 1024
        var accelerators: List<Accelerator> = emptyList()
        
        if (isLlmModel) {
            llmMaxToken = defaultConfig.maxTokens
            if (defaultConfig.accelerators.isNotEmpty()) {
                val items = defaultConfig.accelerators.split(",")
                val accelList = mutableListOf<Accelerator>()
                for (item in items) {
                    when (item.trim().lowercase()) {
                        "cpu" -> accelList.add(Accelerator.CPU)
                        "gpu" -> accelList.add(Accelerator.GPU)
                        "npu" -> accelList.add(Accelerator.NPU)
                    }
                }
                accelerators = accelList
            }
        }

        return AiModel(
            name = name,
            displayName = name,
            version = version,
            info = description,
            url = downloadUrl,
            sizeInBytes = sizeInBytes,
            minDeviceMemoryInGb = minDeviceMemoryInGb,
            downloadFileName = downloadedFileName,
            llmSupportImage = llmSupportImage,
            llmSupportAudio = llmSupportAudio,
            llmMaxToken = llmMaxToken,
            accelerators = accelerators,
            bestForTaskIds = bestForTaskTypes,
            isLlm = isLlmModel
        )
    }
}