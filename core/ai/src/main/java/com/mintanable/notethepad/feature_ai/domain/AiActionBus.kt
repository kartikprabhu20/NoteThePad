package com.mintanable.notethepad.feature_ai.domain

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed class AiAction {
    data class Search(val query: String) : AiAction()
    data class FilterByColor(val colorCode: Int?) : AiAction()
    data class FilterByTag(val tagId: String, val tagName: String) : AiAction()
    object ClearFilters : AiAction()
}

@Singleton
class AiActionBus @Inject constructor() {
    private val _events = MutableSharedFlow<AiAction>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    suspend fun emit(action: AiAction) {
        _events.emit(action)
    }

    fun tryEmit(action: AiAction) {
        _events.tryEmit(action)
    }
}
