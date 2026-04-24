package com.mintanable.notethepad.feature_note.presentation.modify

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

open class BaseSnapshotTracker<T>(private val limit: Int = 50) {
    private val undoStack = ArrayDeque<T>()
    private val redoStack = ArrayDeque<T>()
    private var isApplyingSnapshot = false

    private val _canUndo = MutableStateFlow(false)
    val canUndo = _canUndo.asStateFlow()
    private val _canRedo = MutableStateFlow(false)
    val canRedo = _canRedo.asStateFlow()

    fun applySnapshot(snapshot: T) {
        if (isApplyingSnapshot) return

        if (undoStack.isNotEmpty() && undoStack.last() == snapshot) return

        undoStack.addLast(snapshot)
        if (undoStack.size > limit) {
            undoStack.removeFirst()
        }

        redoStack.clear()
        updateStates()
    }

    fun undo(): T? {
        if (undoStack.size < 2) return null

        isApplyingSnapshot = true

        val stateToSaveToRedo = undoStack.removeLast()
        redoStack.addLast(stateToSaveToRedo)

        val previousState = undoStack.last()

        updateStates()
        isApplyingSnapshot = false
        return previousState
    }

    fun redo(): T? {
        if (redoStack.isEmpty()) return null

        isApplyingSnapshot = true
        val nextState = redoStack.removeLast()

        undoStack.addLast(nextState)

        updateStates()
        isApplyingSnapshot = false
        return nextState
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
        updateStates()
    }

    private fun updateStates() {
        _canUndo.value = undoStack.size >= 2
        _canRedo.value = redoStack.isNotEmpty()
    }
}
