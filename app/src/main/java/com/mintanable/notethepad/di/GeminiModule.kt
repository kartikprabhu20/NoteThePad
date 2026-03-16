package com.mintanable.notethepad.di

import android.content.Context
import com.mintanable.notethepad.BuildConfig
import com.mintanable.notethepad.feature_ai.data.source.GeminiDataSource
import com.mintanable.notethepad.feature_ai.data.repository.NoteAssistantRepositoryImpl
import com.mintanable.notethepad.feature_ai.data.repository.AiModelRepositoryImpl
import com.mintanable.notethepad.feature_ai.data.source.GemmaLocalDataSource
import com.mintanable.notethepad.feature_ai.data.source.NanoDataSource
import com.mintanable.notethepad.feature_ai.domain.repository.NoteAssistantRepository
import com.mintanable.notethepad.feature_ai.domain.repository.AiModelRepository
import com.mintanable.notethepad.feature_note.domain.util.DispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object GeminiModule {

    @Provides
    @Singleton
    fun provideGeminiApiKey(): String {
        return BuildConfig.GEMINI_API_KEY
    }

    @Provides
    @Singleton
    fun provideGeminiDataSource(apiKey: String): GeminiDataSource {
        return GeminiDataSource(apiKey)
    }

    @Provides
    @Singleton
    fun provideAiModelRepository(
        @ApplicationContext context: Context,
        dispatcherProvider: DispatcherProvider)
    : AiModelRepository {
        return AiModelRepositoryImpl(context, dispatcherProvider)
    }

    @Provides
    @Singleton
    fun provideNoteAssistantRepository(
        geminiDataSource: GeminiDataSource,
        gemmaLocalDataSource: GemmaLocalDataSource,
        nanoDataSource: NanoDataSource,
        aiModelRepository: AiModelRepository
    ): NoteAssistantRepository {
        return NoteAssistantRepositoryImpl(
            geminiDataSource,
            gemmaLocalDataSource,
            nanoDataSource,
            aiModelRepository
        )
    }

}