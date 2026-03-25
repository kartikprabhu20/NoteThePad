package com.mintanable.notethepad.core.common.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.floor

const val SAMPLE_RATE: Int = 16000
const val TAG: String =  "kptest"

fun convertWavToMonoWithMaxSeconds(
    context: Context,
    stereoUri: Uri,
    maxSeconds: Int = 30,
): ByteArray? {
    Log.d(TAG, "Start to convert wav file to mono channel")

    try {
        val inputStream = (if (stereoUri.scheme == null || stereoUri.scheme == "file") {
                FileInputStream(stereoUri.path ?: "")
            } else {
                context.contentResolver.openInputStream(stereoUri)
            }) ?: return null
        val originalBytes = inputStream.readBytes()
        inputStream.close()

        // Read WAV header
        if (originalBytes.size < 44) {
            Log.e(TAG, "Not a valid wav file")
            return null
        }

        val headerBuffer = ByteBuffer.wrap(originalBytes, 0, 44).order(ByteOrder.LITTLE_ENDIAN)
        val channels = headerBuffer.getShort(22)
        var sampleRate = headerBuffer.getInt(24)
        val bitDepth = headerBuffer.getShort(34)
        Log.d(TAG, "File metadata: channels: $channels, sampleRate: $sampleRate, bitDepth: $bitDepth")

        // Normalize audio to 16-bit.
        val audioDataBytes = originalBytes.copyOfRange(fromIndex = 44, toIndex = originalBytes.size)
        var sixteenBitBytes: ByteArray =
            if (bitDepth.toInt() == 8) {
                Log.d(TAG, "Converting 8-bit audio to 16-bit.")
                convert8BitTo16Bit(audioDataBytes)
            } else {
                audioDataBytes
            }

        // Convert byte array to short array for processing
        val shortBuffer = ByteBuffer.wrap(sixteenBitBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        var pcmSamples = ShortArray(shortBuffer.remaining())
        shortBuffer.get(pcmSamples)

        // Resample if sample rate is less than 16000 Hz ---
        if (sampleRate < SAMPLE_RATE) {
            Log.d(TAG, "Resampling from $sampleRate Hz to $SAMPLE_RATE Hz.")
            pcmSamples = resample(pcmSamples, sampleRate, SAMPLE_RATE, channels.toInt())
            sampleRate = SAMPLE_RATE
            Log.d(TAG, "Resampling complete. New sample count: ${pcmSamples.size}")
        }

        // Convert stereo to mono if necessary
        var monoSamples =
            if (channels.toInt() == 2) {
                Log.d(TAG, "Converting stereo to mono.")
                val mono = ShortArray(pcmSamples.size / 2)
                for (i in mono.indices) {
                    val left = pcmSamples[i * 2]
                    val right = pcmSamples[i * 2 + 1]
                    mono[i] = ((left + right) / 2).toShort()
                }
                mono
            } else {
                Log.d(TAG, "Audio is already mono. No channel conversion needed.")
                pcmSamples
            }

        // Trim the audio to maxSeconds ---
        val maxSamples = maxSeconds * sampleRate
        if (monoSamples.size > maxSamples) {
            Log.d(TAG, "Trimming clip from ${monoSamples.size} samples to $maxSamples samples.")
            monoSamples = monoSamples.copyOfRange(0, maxSamples)
        }

        val monoByteBuffer = ByteBuffer.allocate(monoSamples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        monoByteBuffer.asShortBuffer().put(monoSamples)
        return monoByteBuffer.array()
    } catch (e: Exception) {
        Log.e(TAG, "Failed to convert wav to mono", e)
        return null
    }
}

/** Converts 8-bit unsigned PCM audio data to 16-bit signed PCM. */
private fun convert8BitTo16Bit(eightBitData: ByteArray): ByteArray {
    // The new 16-bit data will be twice the size
    val sixteenBitData = ByteArray(eightBitData.size * 2)
    val buffer = ByteBuffer.wrap(sixteenBitData).order(ByteOrder.LITTLE_ENDIAN)

    for (byte in eightBitData) {
        // Convert the unsigned 8-bit byte (0-255) to a signed 16-bit short (-32768 to 32767)
        // 1. Get the unsigned value by masking with 0xFF
        // 2. Subtract 128 to center the waveform around 0 (range becomes -128 to 127)
        // 3. Scale by 256 to expand to the 16-bit range
        val unsignedByte = byte.toInt() and 0xFF
        val sixteenBitSample = ((unsignedByte - 128) * 256).toShort()
        buffer.putShort(sixteenBitSample)
    }
    return sixteenBitData
}


/** Resamples PCM audio data from an original sample rate to a target sample rate. */
private fun resample(
    inputSamples: ShortArray,
    originalSampleRate: Int,
    targetSampleRate: Int,
    channels: Int,
): ShortArray {
    if (originalSampleRate == targetSampleRate) {
        return inputSamples
    }

    val ratio = targetSampleRate.toDouble() / originalSampleRate
    val outputLength = (inputSamples.size * ratio).toInt()
    val resampledData = ShortArray(outputLength)

    if (channels == 1) { // Mono
        for (i in resampledData.indices) {
            val position = i / ratio
            val index1 = floor(position).toInt()
            val index2 = index1 + 1
            val fraction = position - index1

            val sample1 = if (index1 < inputSamples.size) inputSamples[index1].toDouble() else 0.0
            val sample2 = if (index2 < inputSamples.size) inputSamples[index2].toDouble() else 0.0

            resampledData[i] = (sample1 * (1 - fraction) + sample2 * fraction).toInt().toShort()
        }
    }

    return resampledData
}

fun ByteArray.genByteArrayForWav(): ByteArray {
    val sampleRate = SAMPLE_RATE
    val header = ByteArray(44)

    val pcmDataSize = this.size
    val wavFileSize = pcmDataSize + 44 // 44 bytes for the header
    val channels = 1 // Mono
    val bitsPerSample: Short = 16
    val byteRate = sampleRate * channels * bitsPerSample / 8
    Log.d(TAG, "Wav metadata: sampleRate: $sampleRate")

    // RIFF/WAVE header
    header[0] = 'R'.code.toByte()
    header[1] = 'I'.code.toByte()
    header[2] = 'F'.code.toByte()
    header[3] = 'F'.code.toByte()
    header[4] = (wavFileSize and 0xff).toByte()
    header[5] = (wavFileSize shr 8 and 0xff).toByte()
    header[6] = (wavFileSize shr 16 and 0xff).toByte()
    header[7] = (wavFileSize shr 24 and 0xff).toByte()
    header[8] = 'W'.code.toByte()
    header[9] = 'A'.code.toByte()
    header[10] = 'V'.code.toByte()
    header[11] = 'E'.code.toByte()
    header[12] = 'f'.code.toByte()
    header[13] = 'm'.code.toByte()
    header[14] = 't'.code.toByte()
    header[15] = ' '.code.toByte()
    header[16] = 16
    header[17] = 0
    header[18] = 0
    header[19] = 0 // Sub-chunk size (16 for PCM)
    header[20] = 1
    header[21] = 0 // Audio format (1 for PCM)
    header[22] = channels.toByte()
    header[23] = 0 // Number of channels
    header[24] = (sampleRate and 0xff).toByte()
    header[25] = (sampleRate shr 8 and 0xff).toByte()
    header[26] = (sampleRate shr 16 and 0xff).toByte()
    header[27] = (sampleRate shr 24 and 0xff).toByte()
    header[28] = (byteRate and 0xff).toByte()
    header[29] = (byteRate shr 8 and 0xff).toByte()
    header[30] = (byteRate shr 16 and 0xff).toByte()
    header[31] = (byteRate shr 24 and 0xff).toByte()
    header[32] = (channels * bitsPerSample / 8).toByte()
    header[33] = 0 // Block align
    header[34] = bitsPerSample.toByte()
    header[35] = (bitsPerSample.toInt() shr 8 and 0xff).toByte() // Bits per sample
    header[36] = 'd'.code.toByte()
    header[37] = 'a'.code.toByte()
    header[38] = 't'.code.toByte()
    header[39] = 'a'.code.toByte()
    header[40] = (pcmDataSize and 0xff).toByte()
    header[41] = (pcmDataSize shr 8 and 0xff).toByte()
    header[42] = (pcmDataSize shr 16 and 0xff).toByte()
    header[43] = (pcmDataSize shr 24 and 0xff).toByte()

    return header + this
}
