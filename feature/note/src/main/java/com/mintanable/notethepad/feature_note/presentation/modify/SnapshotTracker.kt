package com.mintanable.notethepad.feature_note.presentation.modify

import com.mintanable.notethepad.feature_note.presentation.EditNoteSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SnapshotTracker @Inject constructor() {
    private val undoStack = ArrayDeque<EditNoteSnapshot>()
    private val redoStack = ArrayDeque<EditNoteSnapshot>()
    private val undoLimit = 50
    private var isApplyingSnapshot = false

    private val _canUndo = MutableStateFlow(false)
    val canUndo = _canUndo.asStateFlow()
    private val _canRedo = MutableStateFlow(false)
    val canRedo = _canRedo.asStateFlow()

    fun applySnapshot(snapshot: EditNoteSnapshot) {
        if (isApplyingSnapshot) return

        if (undoStack.isNotEmpty() && undoStack.last() == snapshot) return

        undoStack.addLast(snapshot)
        if (undoStack.size > undoLimit) {
            undoStack.removeFirst()
        }

        redoStack.clear()
        updateStates()
    }

    fun undo(): EditNoteSnapshot? {
        if (undoStack.size < 2) return null

        isApplyingSnapshot = true

        val stateToSaveToRedo = undoStack.removeLast()
        redoStack.addLast(stateToSaveToRedo)

        val previousState = undoStack.last()

        updateStates()
        isApplyingSnapshot = false
        return previousState
    }

    fun redo(): EditNoteSnapshot? {
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