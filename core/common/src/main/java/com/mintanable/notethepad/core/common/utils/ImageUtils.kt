package com.mintanable.notethepad.core.common.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Reads an image from a URI, downsamples it efficiently (two-pass decode),
 * applies EXIF rotation, resizes to exactly [maxDimension], and returns
 * JPEG-compressed bytes ready for LLM inference.
 *
 * Memory-critical design choices :
 *  - Stream-based decode (never loads raw file bytes into a ByteArray)
 *  - RGB_565 pixel format (2 bytes/pixel vs 4 bytes for ARGB_8888 — halves bitmap memory)
 *  - inSampleSize pre-scaling + exact final resize
 *  - JPEG output (5-10x smaller than PNG)
 *  - Immediate recycle of every intermediate bitmap
 */
fun readAndProcessImage(context: Context, uri: Uri, maxDimension: Int = 384): ByteArray? {
    val orientation = try {
        openUriStream(context, uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL
    } catch (_: Exception) {
        ExifInterface.ORIENTATION_NORMAL
    }

    val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    openUriStream(context, uri)?.use { BitmapFactory.decodeStream(it, null, boundsOptions) }

    if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) return null

    val sampleSize = calculateInSampleSize(boundsOptions, maxDimension, maxDimension)
    val decodeOptions = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.RGB_565   // 2 bytes/pixel instead of 4
    }
    var bitmap = openUriStream(context, uri)?.use {
        BitmapFactory.decodeStream(it, null, decodeOptions)
    } ?: return null

    bitmap = rotateBitmap(bitmap, orientation)
    bitmap = resizeBitmap(bitmap, maxDimension)
    val out = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
    bitmap.recycle()
    return out.toByteArray()
}

/**
 * Legacy extension kept for compatibility — operates on raw byte arrays.
 * Now includes RGB_565, final resize, and JPEG compression.
 */
fun ByteArray.downscaleImage(maxDimension: Int): ByteArray {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(this, 0, this.size, options)

    val width = options.outWidth
    val height = options.outHeight
    if (width <= maxDimension && height <= maxDimension) return this

    var sampleSize = 1
    while (width / sampleSize > maxDimension || height / sampleSize > maxDimension) {
        sampleSize *= 2
    }

    val decodeOptions = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.RGB_565
    }
    var bitmap = BitmapFactory.decodeByteArray(this, 0, this.size, decodeOptions)
        ?: return this

    bitmap = resizeBitmap(bitmap, maxDimension)

    val out = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
    bitmap.recycle()
    return out.toByteArray()
}

private fun openUriStream(context: Context, uri: Uri): InputStream? =
    if (uri.scheme == "content") {
        context.contentResolver.openInputStream(uri)
    } else {
        val path = uri.path ?: uri.toString()
        try { FileInputStream(path) } catch (_: Exception) { null }
    }

private fun calculateInSampleSize(
    options: BitmapFactory.Options,
    reqWidth: Int,
    reqHeight: Int,
): Int {
    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        val heightRatio = (height.toFloat() / reqHeight.toFloat()).roundToInt()
        val widthRatio = (width.toFloat() / reqWidth.toFloat()).roundToInt()
        inSampleSize = max(heightRatio, widthRatio)
    }
    return inSampleSize
}

private fun resizeBitmap(originalBitmap: Bitmap, size: Int): Bitmap {
    val w = originalBitmap.width
    val h = originalBitmap.height
    if (w <= size && h <= size) return originalBitmap

    val aspectRatio = w.toFloat() / h.toFloat()
    val newWidth: Int
    val newHeight: Int
    if (aspectRatio > 1) {
        newWidth = size
        newHeight = (size / aspectRatio).toInt()
    } else {
        newHeight = size
        newWidth = (size * aspectRatio).toInt()
    }
    val scaled = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
    if (scaled !== originalBitmap) originalBitmap.recycle()
    return scaled
}

private fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1.0f, 1.0f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1.0f, -1.0f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.postRotate(90f)
            matrix.preScale(-1.0f, 1.0f)
        }
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.postRotate(270f)
            matrix.preScale(-1.0f, 1.0f)
        }
        else -> return bitmap
    }
    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    if (rotated !== bitmap) bitmap.recycle()
    return rotated
}
