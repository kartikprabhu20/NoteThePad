package com.mintanable.notethepad.feature_ai.di

import android.content.Context
import com.mintanable.notethepad.core.common.DispatcherProvider
import com.mintanable.notethepad.database.repository.NoteRepository
import com.mintanable.notethepad.feature_ai.BuildConfig
import com.mintanable.notethepad.feature_ai.data.repository.AiModelRepositoryImpl
import com.mintanable.notethepad.feature_ai.data.repository.NoteAssistantRepositoryImpl
import com.mintanable.notethepad.feature_ai.data.source.GeminiDataSource
import com.mintanable.notethepad.feature_ai.data.source.GeminiNanoDataSource
import com.mintanable.notethepad.feature_ai.data.source.GemmaLocalDataSource
import com.mintanable.notethepad.feature_ai.domain.repository.AiModelRepository
import com.mintanable.notethepad.feature_ai.domain.repository.NoteAssistantRepository
import com.mintanable.notethepad.feature_ai.domain.use_cases.GetAutoTagsUseCase
import com.mintanable.notethepad.preference.repository.UserPreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    @Provides
    @Singleton
    fun provideGeminiApiKey(): String = BuildConfig.GEMINI_API_KEY

    @Provides
    @Singleton
    fun provideGeminiDataSource(apiKey: String): GeminiDataSource = GeminiDataSource(apiKey)

    @Provides
    @Singleton
    fun provideAiModelRepository(
        @ApplicationContext context: Context,
        dispatcherProvider: DispatcherProvider
    ): AiModelRepository {
        return AiModelRepositoryImpl(context, dispatcherProvider)
    }

    @Provides
    @Singleton
    fun provideNoteAssistantRepository(
        geminiDataSource: GeminiDataSource,
        gemmaLocalDataSource: GemmaLocalDataSource,
        geminiNanoDataSource: GeminiNanoDataSource,
        aiModelRepository: AiModelRepository
    ): NoteAssistantRepository {
        return NoteAssistantRepositoryImpl(
            geminiDataSource,
            gemmaLocalDataSource,
            geminiNanoDataSource,
            aiModelRepository
        )
    }

    @Provides
    @Singleton
    fun provideGetAutoTagsUseCase(assistantRepository: NoteAssistantRepository, noteRepository: NoteRepository, userPreferencesRepository: UserPreferencesRepository): GetAutoTagsUseCase {
        return GetAutoTagsUseCase(assistantRepository, noteRepository, userPreferencesRepository)
    }
}
