package com.mintanable.notethepad.feature_ai.data.source

import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.audio.AudioSource
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.speechrecognition.SpeechRecognition
import com.google.mlkit.genai.speechrecognition.SpeechRecognizer
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerOptions
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerRequest
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerResponse
import com.mintanable.notethepad.core.model.ai.AiModelDownloadStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.util.Locale
import javax.inject.Inject


class GeminiNanoDataSource @Inject constructor() {
    private val client = Generation.getClient()

    private val options = SpeechRecognizerOptions.builder().apply {
        locale = Locale.US
        preferredMode = SpeechRecognizerOptions.Mode.MODE_ADVANCED
    }.build()

    private var _speechRecognizer: SpeechRecognizer? = null
    private fun getRecognizer(): SpeechRecognizer {
        return _speechRecognizer ?: SpeechRecognition.getClient(options).also { _speechRecognizer = it }
    }

    suspend fun generateTags(prompt: String): String? {
        return try {
            client.generateContent(prompt).candidates.firstOrNull()?.text
        } catch (e: Exception) {
            null
        }
    }

    fun checkLocalStatus(): Flow<AiModelDownloadStatus> = flow {
        val mappedStatus = client.checkStatus().toAIModelDownloadStatus()
        emit(mappedStatus)
    }

    private fun Int.toAIModelDownloadStatus(): AiModelDownloadStatus {
        return when (this) {
            FeatureStatus.AVAILABLE -> AiModelDownloadStatus.Ready
            FeatureStatus.DOWNLOADING -> AiModelDownloadStatus.Downloading
            FeatureStatus.DOWNLOADABLE -> AiModelDownloadStatus.Downloadable
            else -> AiModelDownloadStatus.Unavailable
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    suspend fun startTranscriptionStream(onTranscription: (String) -> Unit) {
        val recognizer = getRecognizer()
        try {
            val status = recognizer.checkStatus()
            if (status != FeatureStatus.AVAILABLE) {
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
        val recognizer = _speechRecognizer ?: SpeechRecognition.getClient(options)
        try {
            emit(recognizer.checkStatus().toAIModelDownloadStatus())
        } finally {
            if (recognizer != _speechRecognizer) {
                recognizer.close()
            }
        }
    }

    suspend fun transcribeAudioFile(audioFile: File, onTranscription: (String) -> Unit) {
        val pfd = ParcelFileDescriptor.open(audioFile, ParcelFileDescriptor.MODE_READ_ONLY)

        val request = SpeechRecognizerRequest.builder().apply {
            audioSource = AudioSource.fromPfd(pfd)
        }.build()
        val recognizer = getRecognizer()

        try {
            recognizer.startRecognition(request).collect { response ->
                when (response) {
                    is SpeechRecognizerResponse.PartialTextResponse -> {
                        onTranscription(response.text)
                    }
                    is SpeechRecognizerResponse.FinalTextResponse -> {
                        onTranscription(response.text)
                        // You can close the PFD once the final result is received
                        pfd.close()
                    }
                    is SpeechRecognizerResponse.ErrorResponse -> {
                        Log.e("kptest", "File transcription error: ${response.e}")
                        pfd.close()
                    }
                    else -> {}
                }
            }
        } catch (e: Exception) {
            pfd.close()
            Log.e("kptest", "Failed to process file: ${e.message}")
        }
    }
}
