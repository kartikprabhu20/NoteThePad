package com.mintanable.notethepad.feature_ai.data.source

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.audio.AudioSource
import com.google.mlkit.genai.speechrecognition.SpeechRecognition
import com.google.mlkit.genai.speechrecognition.SpeechRecognizer
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerOptions
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerRequest
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerResponse
import com.mintanable.notethepad.core.model.ai.AiModelDownloadStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Locale

open class GenericDataSource {

    private var activeOptions: SpeechRecognizerOptions? = null
    var _speechRecognizer: SpeechRecognizer? = null

    protected fun buildOptions(mode: Int): SpeechRecognizerOptions {
        return SpeechRecognizerOptions.builder().apply {
            locale = Locale.US
            preferredMode = mode
        }.build()
    }

    private suspend fun checkStatus(mode: Int): Int {
        val tempOptions = buildOptions(mode)
        val tempClient = SpeechRecognition.getClient(tempOptions)
        return try {
            tempClient.checkStatus()
        } finally {
            tempClient.close()
        }
    }

    fun getRecognizer(options: SpeechRecognizerOptions): SpeechRecognizer {
        return _speechRecognizer ?: SpeechRecognition.getClient(options).also { _speechRecognizer = it }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    open suspend fun startTranscriptionStream(onTranscription: (String) -> Unit) {
        val advancedStatus = checkStatus(SpeechRecognizerOptions.Mode.MODE_ADVANCED)

        val finalMode = if (advancedStatus != FeatureStatus.UNAVAILABLE) {
            Log.d("kptest", "Using ADVANCED mode")
            SpeechRecognizerOptions.Mode.MODE_ADVANCED
        } else {
            Log.d("kptest", "Advanced unavailable ($advancedStatus), falling back to BASIC")
            SpeechRecognizerOptions.Mode.MODE_BASIC
        }

        activeOptions = buildOptions(finalMode)
        val recognizer = getRecognizer(activeOptions!!)

        try {
            val status = recognizer.checkStatus()
            if (status == FeatureStatus.UNAVAILABLE) {
                Log.e("kptest", "SODA Engine not available. Status: $status")
                return
            }

            val request = SpeechRecognizerRequest.builder().apply {
                audioSource = AudioSource.fromMic()
            }.build()

            recognizer.startRecognition(request).collect { response ->
                when (response) {
                    is SpeechRecognizerResponse.FinalTextResponse -> {
                        Log.d("kptest", "FinalTextResponse: ${response.text}")
                        onTranscription(response.text)
                    }
                    is SpeechRecognizerResponse.PartialTextResponse -> {
                        Log.d("kptest", "PartialTextResponse: ${response.text}")
                    }
                    is SpeechRecognizerResponse.ErrorResponse -> {
                        Log.e("kptest", "Error: ${response.e}")
                        return@collect
                    }
                    else -> {}
                }
            }
        } catch (e: Exception) {
            Log.e("kptest", "Transcription stream error", e)
        } finally {
            stopTranscription()
        }
    }

    suspend fun stopTranscription() {
        val recognizer = _speechRecognizer ?: return
        try {
            recognizer.stopRecognition()
            recognizer.close()
        } catch (e: Exception) {
            Log.e("kptes", "Error closing recognizer", e)
        } finally {
            _speechRecognizer = null
        }
    }

    fun checkAudioRecognizerStatus(): Flow<AiModelDownloadStatus> = flow {
        val advancedStatus = checkStatus(SpeechRecognizerOptions.Mode.MODE_ADVANCED)
        if (advancedStatus != FeatureStatus.UNAVAILABLE) {
            emit(advancedStatus.toAIModelDownloadStatus())
        } else {
            val basicStatus = checkStatus(SpeechRecognizerOptions.Mode.MODE_BASIC)
            emit(basicStatus.toAIModelDownloadStatus())
        }
    }

    protected fun Int.toAIModelDownloadStatus(): AiModelDownloadStatus {
        return when (this) {
            FeatureStatus.AVAILABLE -> AiModelDownloadStatus.Ready
            FeatureStatus.DOWNLOADING -> AiModelDownloadStatus.Downloading
            FeatureStatus.DOWNLOADABLE -> AiModelDownloadStatus.Downloadable
            else -> AiModelDownloadStatus.Unavailable
        }
    }
}