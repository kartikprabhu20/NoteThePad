package com.mintanable.notethepad.feature_appfunctions.di

import com.mintanable.notethepad.database.db.repository.NoteRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppFunctionsEntryPoint {
    fun noteRepository(): NoteRepository
}
