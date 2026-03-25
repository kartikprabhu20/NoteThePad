package com.mintanable.notethepad.feature_note.domain.repository

import kotlinx.coroutines.flow.StateFlow
import java.io.File

interface AudioRecorder {
    val amplitude: StateFlow<Int>
    fun startRecording(outputFile: File)
    fun stopRecording()
}
