package com.mintanable.notethepad.feature_ai.domain.use_case

import com.mintanable.notethepad.core.model.AiModel
import com.mintanable.notethepad.feature_ai.domain.repository.AiModelRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSupportedAiModels @Inject constructor(
    private val repository: AiModelRepository
) {
    operator fun invoke(): Flow<List<AiModel>> {
        return repository.getModels()
    }
}
