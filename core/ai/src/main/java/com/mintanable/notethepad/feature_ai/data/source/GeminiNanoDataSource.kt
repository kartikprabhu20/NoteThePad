package com.mintanable.notethepad.feature_ai.data.source

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.audio.AudioSource
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.speechrecognition.SpeechRecognition
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerOptions
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerRequest
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerResponse
import com.mintanable.notethepad.core.model.ai.AiModelDownloadStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject


class GeminiNanoDataSource @Inject constructor() {
    private val client = Generation.getClient()

    val options = SpeechRecognizerOptions.builder().apply {
        locale = Locale.US
        preferredMode = SpeechRecognizerOptions.Mode.MODE_ADVANCED
    }.build()

    val speechRecognizer get() = SpeechRecognition.getClient(options)
    val scope = CoroutineScope(Dispatchers.Main)

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
        val mappedStatus = when (this) {
            FeatureStatus.AVAILABLE -> AiModelDownloadStatus.Ready
            FeatureStatus.DOWNLOADING -> AiModelDownloadStatus.Downloading
            FeatureStatus.DOWNLOADABLE -> AiModelDownloadStatus.Downloadable
            else -> AiModelDownloadStatus.Unavailable
        }
        return mappedStatus
    }

    @RequiresApi(Build.VERSION_CODES.S)
    suspend fun startTranscriptionStream(onTranscription: (String) -> Unit) {

        val status = speechRecognizer.checkStatus()
        if (status != FeatureStatus.AVAILABLE) {
            Log.e("kptest", "SODA Engine not available. Status: $status")
            return
        }

        val request = SpeechRecognizerRequest.builder().apply {
            audioSource =  AudioSource.fromMic()
        }.build()

        speechRecognizer.startRecognition(request).collect { response ->

            when(response){
                is SpeechRecognizerResponse.FinalTextResponse -> {
                    Log.d("kptest", "FinalTextResponse: ${response.text}")
                    onTranscription(response.text)
                }
                is SpeechRecognizerResponse.PartialTextResponse -> {
                    Log.d("kptest", "PartialTextResponse: ${response.text}")
                    onTranscription(response.text)
                }
                is SpeechRecognizerResponse.ErrorResponse -> {
                    Log.d("kptest", "Error: ${response.e}")
                    speechRecognizer.close()
                }

                else -> {}
            }
        }
    }

    fun stopTranscription() {
        scope.launch {
            speechRecognizer.stopRecognition()
            speechRecognizer.close()
        }
    }

     fun checkAudioRecognizerStatus(): Flow<AiModelDownloadStatus> =  flow {
       emit(speechRecognizer.checkStatus().toAIModelDownloadStatus())
    }
}
