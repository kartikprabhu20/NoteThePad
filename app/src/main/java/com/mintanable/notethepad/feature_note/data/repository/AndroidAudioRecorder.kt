package com.mintanable.notethepad.feature_note.data.repository

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import com.mintanable.notethepad.feature_note.domain.repository.AudioRecorder
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class AndroidAudioRecorder @Inject constructor(
    @ApplicationContext private val context: Context
): AudioRecorder {
    private var recorder: MediaRecorder? = null

    override fun startRecording(outputFile: File) {
        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4) // Better compatibility
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)   // High quality, low size
            setOutputFile(outputFile.absolutePath)

            prepare()
            start()
        }
    }

    override fun stopRecording() {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
    }
}