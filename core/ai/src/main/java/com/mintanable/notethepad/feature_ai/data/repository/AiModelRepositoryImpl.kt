package com.mintanable.notethepad.feature_ai.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.mintanable.notethepad.core.common.DispatcherProvider
import com.mintanable.notethepad.core.model.ai.AiModel
import com.mintanable.notethepad.core.model.ai.AiModelCatalog
import com.mintanable.notethepad.feature_ai.BuildConfig
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
import javax.inject.Inject
import javax.inject.Singleton

private const val MODEL_CATALOG_FILENAME = "ai_model_catelog.json"

@Singleton
class AiModelRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatcherProvider: DispatcherProvider
) : AiModelRepository {

    private val _cachedModels = MutableStateFlow<List<AiModel>>(emptyList())
    private val client = OkHttpClient()
    private val gson = Gson()

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
                val updatedModels = getStaticModels() + internalCatalog.models.map { it.toAiModel() }
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
                val finalModels = getStaticModels() + freshCatalog.models.map { it.toAiModel() }
                if (finalModels != _cachedModels.value) {
                    _cachedModels.value = finalModels
                    emit(finalModels)
                }
            }
        } catch (e: Exception) {
            Log.e("kptest", "Error processing remote AI catalog", e)
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

    private suspend fun fetchAndSaveCatalogFromGist() = withContext(dispatcherProvider.io) {
        try {
            val request = Request.Builder()
                .url(BuildConfig.AI_CATELOG_GIST_URL)
                .addHeader("Authorization", "token ${BuildConfig.GITHUB_GIST_ACCESS_TOKEN}")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val gistResponse = response.body.string()
                    Log.d("kptest", "gistResponse $gistResponse")

                    if (gistResponse.isNotEmpty()) {
                        val wrapper = gson.fromJson(gistResponse, GistWrapper::class.java)
                        val content = wrapper.files[MODEL_CATALOG_FILENAME]?.content

                        if (content != null) {
                            val internalFile = File(context.filesDir, MODEL_CATALOG_FILENAME)
                            internalFile.writeText(content)
                            Log.d("kptest", "Catalog updated from Gist")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("kptest", "Failed to fetch catalog from Gist", e)
        }
    }

    private fun getAiModelCatalogFromInternal(): AiModelCatalog? {
        return try {
            val file = File(context.filesDir, MODEL_CATALOG_FILENAME)
            if (file.exists()) {
                file.readText().let { gson.fromJson(it, AiModelCatalog::class.java) }
            } else null
        } catch (e: Exception) {
            Log.e("kptest", "Failed to parse internal catalog", e)
            null
        }
    }

    private fun getAiModelCatalogFromAssets(): AiModelCatalog? {
        return try {
            context.assets.open(MODEL_CATALOG_FILENAME)
                .bufferedReader()
                .use { it.readText() }
                .let { gson.fromJson(it, AiModelCatalog::class.java) }
        } catch (e: Exception) {
            Log.e("kptest", "Failed to parse assets catalog", e)
            null
        }
    }
}

data class GistWrapper(
    val files: Map<String, GistFile>
)

data class GistFile(
    val content: String?
)
