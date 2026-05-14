package com.mintanable.notethepad.feature_appfunctions

import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.service.AppFunction
import com.mintanable.notethepad.database.db.entity.NoteEntity
import com.mintanable.notethepad.feature_appfunctions.di.AppFunctionsEntryPoint
import dagger.hilt.android.EntryPointAccessors

class NoteAppFunctions {

    @AppFunction
    suspend fun createNote(
        appFunctionContext: AppFunctionContext,
        title: String,
        content: String,
    ): String {
        val noteRepository = EntryPointAccessors
            .fromApplication(
                appFunctionContext.context.applicationContext,
                AppFunctionsEntryPoint::class.java,
            )
            .noteRepository()
        val entity = NoteEntity(
            title = title,
            content = content,
            timestamp = System.currentTimeMillis(),
        )
        return noteRepository.insertNote(entity, emptyList())
    }
}
