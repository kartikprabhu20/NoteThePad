package com.mintanable.notethepad.feature_note.data.repository

import android.Manifest
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import com.mintanable.notethepad.feature_note.domain.repository.AudioRecorder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.RandomAccessFile
import javax.inject.Inject

class AndroidAudioRecorder @Inject constructor(
    @ApplicationContext private val context: Context
): AudioRecorder {
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    // AI Requirements: 16kHz, Mono, 16-bit PCM
    private val SAMPLE_RATE = 16000
    private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    private val BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun startRecording(outputFile: File) {

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION, // Optimized for AI/STT
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            BUFFER_SIZE
        )

        audioRecord?.startRecording()

        recordingJob = scope.launch {
            outputFile.outputStream().use { out ->
                // Leave space for the 44-byte WAV header
                out.write(ByteArray(44))

                val buffer = ByteArray(BUFFER_SIZE)
                while (isActive && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) out.write(buffer, 0, read)
                }

                // Once stopped, go back and write the proper WAV header
                writeWavHeader(outputFile)
            }
        }
    }

    private fun writeWavHeader(file: File) {
        val totalAudioLen = file.length() - 44
        val totalDataLen = totalAudioLen + 36
        val channels = 1
        val byteRate = SAMPLE_RATE * channels * 2 // 16-bit = 2 bytes

        val header = createWavHeader(totalAudioLen, totalDataLen, SAMPLE_RATE, channels, byteRate)

        RandomAccessFile(file, "rw").use { raf ->
            raf.seek(0)
            raf.write(header)
        }
    }

    override fun stopRecording() {
        audioRecord?.apply {
            stop()
            release()
        }
        audioRecord = null
    }

    private fun createWavHeader(audioLen: Long, dataLen: Long, sampleRate: Int, channels: Int, byteRate: Int): ByteArray {
        val header = ByteArray(44)
        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        header[4] = (dataLen and 0xff).toByte(); header[5] = (dataLen shr 8 and 0xff).toByte()
        header[6] = (dataLen shr 16 and 0xff).toByte(); header[7] = (dataLen shr 24 and 0xff).toByte()
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0; // Sub-chunk size
        header[20] = 1; header[21] = 0; // PCM format
        header[22] = channels.toByte(); header[23] = 0
        header[24] = (sampleRate and 0xff).toByte(); header[25] = (sampleRate shr 8 and 0xff).toByte()
        header[26] = (sampleRate shr 16 and 0xff).toByte(); header[27] = (sampleRate shr 24 and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte(); header[29] = (byteRate shr 8 and 0xff).toByte()
        header[30] = (byteRate shr 16 and 0xff).toByte(); header[31] = (byteRate shr 24 and 0xff).toByte()
        header[32] = (channels * 2).toByte(); header[33] = 0; // Block align
        header[34] = 16; header[35] = 0; // Bits per sample
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        header[40] = (audioLen and 0xff).toByte(); header[41] = (audioLen shr 8 and 0xff).toByte()
        header[42] = (audioLen shr 16 and 0xff).toByte(); header[43] = (audioLen shr 24 and 0xff).toByte()
        return header
    }
}
