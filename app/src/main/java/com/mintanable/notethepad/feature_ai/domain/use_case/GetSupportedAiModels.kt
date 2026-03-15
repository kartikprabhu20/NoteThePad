package com.mintanable.notethepad.feature_ai.domain.use_case

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.mintanable.notethepad.feature_ai.domain.model.AiModel
import com.mintanable.notethepad.feature_ai.domain.model.AiModelCatalog
import com.mintanable.notethepad.feature_note.domain.util.DispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

private const val MODEL_ALLOWLIST_FILENAME = "ai_model_catelog.json"

class GetSupportedAiModels @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatcherProvider: DispatcherProvider
) {
    operator fun invoke(): Flow<List<AiModel>> = flow {
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
            emit(staticModels + domainModels)
        } catch (e: Exception) {
            Log.e("kptest", "Error processing AI catalog", e)
            emit(staticModels)
        }
    }.flowOn(dispatcherProvider.io)

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