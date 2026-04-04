package com.mintanable.notethepad.feature_ai.domain.use_cases

import android.content.Context
import com.mintanable.notethepad.core.model.ai.AiModel
import com.mintanable.notethepad.feature_ai.domain.repository.AiModelRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject

class GetSupportedAiModels @Inject constructor(
    @ApplicationContext val context: Context,
    private val repository: AiModelRepository
) {
    operator fun invoke(): Flow<List<AiModel>> {
        val modelsFlow = repository.getModels().map { models ->
            val updatedModels = models.map { model ->
                if (model.url.isEmpty()) return@map model
                val externalFilesDir = context.getExternalFilesDir(null)
                val modelFile = externalFilesDir?.let { File(it, model.downloadFileName) }
                val isDownloaded =
                    modelFile?.exists() == true && modelFile.length() == model.sizeInBytes
                model.copy(isDownloaded = isDownloaded)
            }
            updatedModels
        }

        return modelsFlow
    }
}
