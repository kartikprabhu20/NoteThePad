package com.mintanable.notethepad.feature_ai.domain.use_case

import com.mintanable.notethepad.feature_ai.domain.model.AiModel
import com.mintanable.notethepad.feature_ai.domain.repository.AiModelRepository
import javax.inject.Inject

class GetAiModelByName @Inject constructor(
    private val repository: AiModelRepository
) {
    suspend operator fun invoke(name: String): AiModel? {
        return repository.getModelByName(name)
    }
}
