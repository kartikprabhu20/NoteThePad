package com.mintanable.notethepad.feature_widgets.presentation

import com.mintanable.notethepad.feature_note.domain.use_case.NoteUseCases
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun noteUseCases(): NoteUseCases
}