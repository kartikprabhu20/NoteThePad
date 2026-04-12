package com.mintanable.notethepad.feature_ai.data.repository

import android.content.Context
import android.util.Log
import com.mintanable.notethepad.core.common.DispatcherProvider
import com.mintanable.notethepad.core.model.ai.AiModel
import com.mintanable.notethepad.core.model.ai.AiModelCatalog
import com.mintanable.notethepad.feature_ai.BuildConfig
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.mintanable.notethepad.feature_ai.domain.repository.AiModelRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val MODEL_CATALOG_FILENAME = "ai_model_catelog.json"

@Singleton
class AiModelRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatcherProvider: DispatcherProvider
) : AiModelRepository {

    private val _cachedModels = MutableStateFlow<List<AiModel>>(emptyList())
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    override fun getModels(): Flow<List<AiModel>> = flow {
        if (_cachedModels.value.isNotEmpty()) {
            emit(_cachedModels.value)
        } else {
            // Initial load from assets if cache is empty
            val catalog = getAiModelCatalogFromAssets()
            val initialModels = getStaticModels() + (catalog?.models?.map { it.toAiModel() } ?: emptyList())
            _cachedModels.value = initialModels
            emit(initialModels)
        }

        try {
            // Try to load from internal storage first
            val internalCatalog = getAiModelCatalogFromInternal()
            if (internalCatalog != null) {
                val updatedModels = getStaticModels() + (internalCatalog.models?.map { it.toAiModel() } ?: emptyList())
                if (updatedModels != _cachedModels.value) {
                    _cachedModels.value = updatedModels
                    emit(updatedModels)
                }
            }

            // Background update from Gist
            fetchAndSaveCatalogFromGist()

            // After Gist fetch, check if internal storage has new data
            val freshCatalog = getAiModelCatalogFromInternal()
            if (freshCatalog != null) {
                val finalModels = getStaticModels() + (freshCatalog.models?.map { it.toAiModel() } ?: emptyList())
                if (finalModels != _cachedModels.value) {
                    _cachedModels.value = finalModels
                    emit(finalModels)
                }
            }
        } catch (e: Exception) {
            Log.e("kptest", "Error processing remote AI catalog", e)
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }.flowOn(dispatcherProvider.io)

    override suspend fun getModelByName(name: String): AiModel? {
        if (_cachedModels.value.isEmpty()) {
            getModels().first()
        }
        return _cachedModels.value.find { it.name == name }
    }

    override suspend fun refreshModels() {
        fetchAndSaveCatalogFromGist()
        val freshCatalog = getAiModelCatalogFromInternal() ?: getAiModelCatalogFromAssets()
        val finalModels = getStaticModels() + (freshCatalog?.models?.map { it.toAiModel() } ?: emptyList())
        _cachedModels.value = finalModels
    }

    private fun getStaticModels() = listOf(
        AiModel(
            name = "None",
            displayName = "None",
            info = "No AI assistance will be provided.",
        ),
//        AiModel(
//            name = "Gemini 3 Flash (Cloud)",
//            displayName = "Gemini 3 Flash (Cloud)",
//            info = "Fastest response, requires internet connection."
//        ),
        AiModel(
            name = "Gemini Nano (System)",
            displayName = "Gemini Nano (System)",
            info = "On-device privacy and performance for text and audio features. Live transcription is key feature. Only on supported devices.",
        )
    )

    private suspend fun fetchAndSaveCatalogFromGist() = withContext(dispatcherProvider.io) {
        try {
            val request = Request.Builder()
                .url(BuildConfig.AI_CATELOG_GIST_URL)
                .addHeader("Authorization", "token ${BuildConfig.GIST_ACCESS_TOKEN}")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val gistResponse = response.body.string()

                    if (!gistResponse.isNullOrEmpty()) {
                        try {
                            val wrapper = json.decodeFromString<GistWrapper>(gistResponse)
                            val content = wrapper.files?.get(MODEL_CATALOG_FILENAME)?.content

                            if (content != null) {
                                val internalFile = File(context.filesDir, MODEL_CATALOG_FILENAME)
                                internalFile.writeText(content)
                                Log.d("kptest", "Catalog updated from Gist")
                            } else {
                                Log.w("kptest", "Catalog content is null for $MODEL_CATALOG_FILENAME")
                                FirebaseCrashlytics.getInstance().log("Gist catalog content is null for $MODEL_CATALOG_FILENAME")
                            }
                        } catch (e: Exception) {
                            Log.e("kptest", "Failed to parse Gist response", e)
                            FirebaseCrashlytics.getInstance().apply {
                                log("Failed to parse Gist response: $gistResponse")
                                recordException(e)
                            }
                        }
                    }
                } else {
                    Log.e("kptest", "Gist fetch unsuccessful: ${response.code}")
                    FirebaseCrashlytics.getInstance().log("Gist fetch unsuccessful: ${response.code}")
                }
            }
        } catch (e: Exception) {
            Log.e("kptest", "Failed to fetch catalog from Gist $e")
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    private fun getAiModelCatalogFromInternal(): AiModelCatalog? {
        return try {
            val file = File(context.filesDir, MODEL_CATALOG_FILENAME)
            if (file.exists()) {
                file.readText().let { json.decodeFromString<AiModelCatalog>(it) }
            } else null
        } catch (e: Exception) {
            Log.e("kptest", "Failed to parse internal catalog", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            null
        }
    }

    private fun getAiModelCatalogFromAssets(): AiModelCatalog? {
        return try {
            context.assets.open(MODEL_CATALOG_FILENAME)
                .bufferedReader()
                .use { it.readText() }
                .let { json.decodeFromString<AiModelCatalog>(it) }
        } catch (e: Exception) {
            Log.e("kptest", "Failed to parse assets catalog", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            null
        }
    }
}

@Serializable
data class GistWrapper(
    @SerialName("files") val files: Map<String, GistFile>? = null
)

@Serializable
data class GistFile(
    @SerialName("content") val content: String? = null
)
