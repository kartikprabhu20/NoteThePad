package com.mintanable.notethepad.feature_ai.domain.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class AiModelCatalog(
    @SerializedName("models") val models: List<AiModelEntry>
)


@Serializable
data class ModelDefaultConfig(
    @SerializedName("topK") val topK: Int,
    @SerializedName("topP") val topP: Float,
    @SerializedName("temperature") val temperature: Float,
    @SerializedName("maxTokens") val maxTokens: Int,
    @SerializedName("accelerators") val accelerators: String
)

@Serializable
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

        var version = commitHash
        var downloadedFileName = modelFile
        var downloadUrl =
            "https://huggingface.co/$modelId/resolve/$commitHash/$modelFile?download=true"
        var sizeInBytes = sizeInBytes

        val isLlmModel =
            taskTypes.contains(BuiltInTaskId.LLM_CHAT) ||
                    taskTypes.contains(BuiltInTaskId.LLM_PROMPT_LAB) ||
                    taskTypes.contains(BuiltInTaskId.LLM_ASK_AUDIO) ||
                    taskTypes.contains(BuiltInTaskId.LLM_ASK_IMAGE)
        var llmMaxToken = 1024
        var accelerators: List<Accelerator> = emptyList()
        if (isLlmModel) {
            val defaultTopK: Int = defaultConfig.topK ?: 32
            val defaultTopP: Float = defaultConfig.topP ?: 0.9f
            val defaultTemperature: Float = defaultConfig.temperature ?: 0.9f
            llmMaxToken = defaultConfig.maxTokens ?: 1024
            if (defaultConfig.accelerators.isNotEmpty()) {
                val items = defaultConfig.accelerators.split(",")
                accelerators = mutableListOf()
                for (item in items) {
                    if (item == "cpu") {
                        accelerators.add(Accelerator.CPU)
                    } else if (item == "gpu") {
                        accelerators.add(Accelerator.GPU)
                    } else if (item == "npu") {
                        accelerators.add(Accelerator.NPU)
                    }
                }
            }
        }

        return AiModel(
            name = name,
            version = version,
            info = description,
            url = downloadUrl,
            sizeInBytes = sizeInBytes,
            minDeviceMemoryInGb = minDeviceMemoryInGb,
            downloadFileName = downloadedFileName,
            llmSupportImage = llmSupportImage == true,
            llmSupportAudio = llmSupportAudio == true,
            llmMaxToken = llmMaxToken,
            accelerators = accelerators,
            bestForTaskIds = bestForTaskTypes ?: emptyList(),
            isLlm = isLlmModel,
        )
    }
}