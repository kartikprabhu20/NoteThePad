package com.mintanable.notethepad.feature_ai.data.source

import android.os.ParcelFileDescriptor
import android.util.Log
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.mlkit.genai.common.audio.AudioSource
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerOptions
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerRequest
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerResponse
import com.google.mlkit.genai.summarization.Summarization
import com.google.mlkit.genai.summarization.SummarizationRequest
import com.google.mlkit.genai.summarization.SummarizerOptions
import com.mintanable.notethepad.core.model.ai.AiModelDownloadStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject


class GeminiNanoDataSource @Inject constructor(
    @ApplicationContext private val context: android.content.Context
): GenericDataSource(){
    private val client = Generation.getClient()

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

    suspend fun transcribeAudioFile(audioFile: File, onTranscription: (String) -> Unit) {
        if (audioFile.length() == 0L) {
            Log.e("kptest", "File is empty!")
            return
        }

        val fileBytes = audioFile.readBytes()
        if (fileBytes.size < 44) {
            Log.e("kptest", "Invalid WAV file: too small")
            return
        }

        // Parse actual format from the WAV header
        val header = ByteBuffer.wrap(fileBytes, 0, 44).order(ByteOrder.LITTLE_ENDIAN)
        val channels = header.getShort(22).toInt()
        val sampleRate = header.getInt(24)
        val bitsPerSample = header.getShort(34).toInt()
        val bytesPerSecond = sampleRate * channels * (bitsPerSample / 8)

        val pcmData = fileBytes.copyOfRange(44, fileBytes.size)
        val chunkSize = CHUNK_DURATION_SECONDS * bytesPerSecond

        Log.d("kptest", "WAV: ${sampleRate}Hz, ${channels}ch, ${bitsPerSample}bit | " +
                "PCM: ${pcmData.size} bytes | chunkSize: $chunkSize bytes")

        if (pcmData.size <= chunkSize) {
            // Short file — use original file directly (proven to work)
            transcribePfd(audioFile, onTranscription)
            return
        }

        // Split into chunks using the original header as template
        val originalHeader = fileBytes.copyOfRange(0, 44)
        val tempDir = File(context.cacheDir, "audio_chunks")
        tempDir.mkdirs()

        var offset = 0
        var chunkIndex = 0
        val totalChunks = (pcmData.size + chunkSize - 1) / chunkSize

        try {
            while (offset < pcmData.size) {
                val end = minOf(offset + chunkSize, pcmData.size)
                val chunkPcm = pcmData.copyOfRange(offset, end)
                val chunkWav = buildChunkWav(originalHeader, chunkPcm)

                val tempFile = File(tempDir, "chunk_$chunkIndex.wav")
                FileOutputStream(tempFile).use { fos ->
                    fos.write(chunkWav)
                    fos.fd.sync()
                }

                Log.d("kptest", "Chunk ${chunkIndex + 1}/$totalChunks: " +
                        "${chunkWav.size} bytes (PCM: ${chunkPcm.size})")

                val callback: (String) -> Unit = if (chunkIndex == 0) {
                    onTranscription
                } else {
                    { text -> onTranscription(" $text") }
                }
                transcribePfd(tempFile, callback)
                tempFile.delete()

                offset = end
                chunkIndex++
            }
        } finally {
            tempDir.deleteRecursively()
        }
    }

    /**
     * Builds a WAV file for a chunk by copying the original file's header
     * and patching only the two size fields.
     */
    private fun buildChunkWav(originalHeader: ByteArray, pcmData: ByteArray): ByteArray {
        val h = originalHeader.copyOf()
        val dataSize = pcmData.size
        val riffSize = dataSize + 36 // file size - 8

        // RIFF chunk size (bytes 4-7, little-endian)
        h[4] = (riffSize and 0xff).toByte()
        h[5] = (riffSize shr 8 and 0xff).toByte()
        h[6] = (riffSize shr 16 and 0xff).toByte()
        h[7] = (riffSize shr 24 and 0xff).toByte()

        // data sub-chunk size (bytes 40-43, little-endian)
        h[40] = (dataSize and 0xff).toByte()
        h[41] = (dataSize shr 8 and 0xff).toByte()
        h[42] = (dataSize shr 16 and 0xff).toByte()
        h[43] = (dataSize shr 24 and 0xff).toByte()

        return h + pcmData
    }

    private suspend fun transcribePfd(file: File, onTranscription: (String) -> Unit) {
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val request = SpeechRecognizerRequest.builder().apply {
            audioSource = AudioSource.fromPfd(pfd)
        }.build()
        val recognizer = getRecognizer(buildOptions(SpeechRecognizerOptions.Mode.MODE_ADVANCED))
        try {
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
                        Log.e("kptest", "Transcription error: ${response.e}")
                    }
                    else -> {}
                }
            }
        } catch (e: Exception) {
            Log.e("kptest", "Failed to process file: ${e.message}")
        } finally {
            pfd.close()
            // Recognizer enters terminal state after each session; must recreate for next chunk
            try { _speechRecognizer?.close() } catch (_: Exception) {}
            _speechRecognizer = null
        }
    }

    @OptIn(ExperimentalApi::class)
    suspend fun summarizeNote(textToSummarize: String): String? = withContext(Dispatchers.IO) {
        val summarizerOptions = SummarizerOptions.builder(context)
            .setInputType(SummarizerOptions.InputType.ARTICLE)
            .setOutputType(SummarizerOptions.OutputType.THREE_BULLETS)
            .setLanguage(SummarizerOptions.Language.ENGLISH)
            .build()

        val summarizationClient = Summarization.getClient(summarizerOptions)
        val summarizationRequest = SummarizationRequest.builder(textToSummarize).build()
        val summarizationResult = summarizationClient.runInference(summarizationRequest).get().summary

        summarizationClient.close()
        return@withContext summarizationResult
    }

    companion object {
        private const val CHUNK_DURATION_SECONDS = 5
    }
}
