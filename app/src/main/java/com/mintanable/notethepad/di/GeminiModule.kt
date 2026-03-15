package com.mintanable.notethepad.di

import com.mintanable.notethepad.BuildConfig
import com.mintanable.notethepad.feature_ai.data.GeminiDataSource
import com.mintanable.notethepad.feature_ai.data.NoteAssistantRepositoryImpl
import com.mintanable.notethepad.feature_ai.domain.NoteAssistantRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
    fun provideNoteAssistantRepository(geminiDataSource: GeminiDataSource): NoteAssistantRepository {
        return NoteAssistantRepositoryImpl(geminiDataSource)
    }

}