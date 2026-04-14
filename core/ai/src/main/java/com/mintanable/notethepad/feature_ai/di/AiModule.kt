package com.mintanable.notethepad.feature_ai.di

import android.content.Context
import com.mintanable.notethepad.core.common.DispatcherProvider
import com.mintanable.notethepad.database.db.repository.NoteRepository
import com.mintanable.notethepad.feature_ai.BuildConfig
import com.mintanable.notethepad.feature_ai.data.repository.AiModelRepositoryImpl
import com.mintanable.notethepad.feature_ai.data.repository.NoteAssistantRepositoryImpl
import com.mintanable.notethepad.feature_ai.data.source.GeminiDataSource
import com.mintanable.notethepad.feature_ai.data.source.GeminiNanoDataSource
import com.mintanable.notethepad.feature_ai.data.source.GemmaLocalDataSource
import com.mintanable.notethepad.feature_ai.domain.repository.AiModelRepository
import com.mintanable.notethepad.feature_ai.domain.repository.NoteAssistantRepository
import com.mintanable.notethepad.feature_ai.domain.use_cases.GetAutoTagsUseCase
import com.mintanable.notethepad.feature_ai.tools.BackupTools
import com.mintanable.notethepad.feature_ai.tools.MediaTools
import com.mintanable.notethepad.feature_ai.tools.NoteTools
import com.mintanable.notethepad.feature_ai.tools.SearchTools
import com.mintanable.notethepad.feature_ai.tools.TagTools
import com.mintanable.notethepad.database.preference.repository.UserPreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    @Suppress("PurposefulConstant")
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
        @ApplicationContext context: Context,
        geminiDataSource: GeminiDataSource,
        gemmaLocalDataSource: GemmaLocalDataSource,
        geminiNanoDataSource: GeminiNanoDataSource,
        aiModelRepository: AiModelRepository,
        noteTools: NoteTools,
        searchTools: SearchTools,
        tagTools: TagTools,
        mediaTools: MediaTools,
        backupTools: BackupTools,
    ): NoteAssistantRepository {
        return NoteAssistantRepositoryImpl(
            context,
            geminiDataSource,
            gemmaLocalDataSource,
            geminiNanoDataSource,
            aiModelRepository,
            noteTools,
            searchTools,
            tagTools,
            mediaTools,
            backupTools,
        )
    }

    @Provides
    @Singleton
    fun provideGetAutoTagsUseCase(assistantRepository: NoteAssistantRepository, noteRepository: NoteRepository, userPreferencesRepository: UserPreferencesRepository): GetAutoTagsUseCase {
        return GetAutoTagsUseCase(assistantRepository, noteRepository, userPreferencesRepository)
    }
}
