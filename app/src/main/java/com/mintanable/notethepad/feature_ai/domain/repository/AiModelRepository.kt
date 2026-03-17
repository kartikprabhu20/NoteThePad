package com.mintanable.notethepad.feature_ai.domain.repository

import com.mintanable.notethepad.feature_ai.domain.model.AiModel
import kotlinx.coroutines.flow.Flow

interface AiModelRepository {
    fun getModels(): Flow<List<AiModel>>
    suspend fun getModelByName(name: String): AiModel?
    suspend fun refreshModels()
}
