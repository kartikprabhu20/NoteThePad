package com.mintanable.notethepad.feature_widgets.presentation.utils

import com.mintanable.notethepad.database.db.repository.NoteRepository
import com.mintanable.notethepad.database.helper.DetailedNoteMapper
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun noteRepository(): NoteRepository
    fun detailedNoteMapper(): DetailedNoteMapper
}