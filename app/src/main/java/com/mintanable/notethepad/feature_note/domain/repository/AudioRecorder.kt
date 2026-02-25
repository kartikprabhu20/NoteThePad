package com.mintanable.notethepad.feature_note.domain.repository

import java.io.File

interface AudioRecorder {
    fun startRecording(outputFile: File)
    fun stopRecording()
}