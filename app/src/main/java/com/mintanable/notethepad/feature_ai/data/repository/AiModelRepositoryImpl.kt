package com.mintanable.notethepad.feature_ai.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.mintanable.notethepad.feature_ai.domain.model.AiModel
import com.mintanable.notethepad.feature_ai.domain.model.AiModelCatalog
import com.mintanable.notethepad.feature_ai.domain.repository.AiModelRepository
import com.mintanable.notethepad.feature_note.domain.util.DispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

private const val MODEL_ALLOWLIST_FILENAME = "ai_model_catelog.json"

@Singleton
class AiModelRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatcherProvider: DispatcherProvider
) : AiModelRepository {

    private val _cachedModels = MutableStateFlow<List<AiModel>>(emptyList())

    override fun getModels(): Flow<List<AiModel>> = flow {
        if (_cachedModels.value.isNotEmpty()) {
            emit(_cachedModels.value)
            return@flow
        }

        val staticModels = listOf(
            AiModel(
                name = "None",
                displayName = "None",
                info = "No AI assistance will be provided.",
            ),
            AiModel(
                name = "Gemini 3 Flash (Cloud)",
                displayName = "Gemini 3 Flash (Cloud)",
                info = "Fastest response, requires internet connection."
            ),
            AiModel(
                name = "Gemini Nano (System)",
                displayName = "Gemini Nano (System)",
                info = "On-device privacy and performance. Only on supported devices.",
            )
        )

        try {
            val catalog = getAiModelCatalog(context, MODEL_ALLOWLIST_FILENAME)
            val domainModels = catalog?.models?.map { it.toAiModel() } ?: emptyList()
            val allModels = staticModels + domainModels
            _cachedModels.value = allModels
            emit(allModels)
        } catch (e: Exception) {
            Log.e("kptest", "Error processing AI catalog", e)
            emit(staticModels)
        }
    }.flowOn(dispatcherProvider.io)

    override suspend fun getModelByName(name: String): AiModel? {
        if (_cachedModels.value.isEmpty()) {
            getModels().onEach { }.collect {}
        }
        return _cachedModels.value.find { it.name == name }
    }

    private fun getAiModelCatalog(context: Context, fileName: String): AiModelCatalog? {
        return try {
            context.assets.open(fileName)
                .bufferedReader()
                .use { it.readText() }
                .let { Gson().fromJson(it, AiModelCatalog::class.java) }
        } catch (e: Exception) {
            Log.e("kptest", "Failed to parse catalog", e)
            null
        }
    }
}
