package com.mintanable.notethepad.core.common.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream

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

    val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    val bitmap = BitmapFactory.decodeByteArray(this, 0, this.size, decodeOptions)
        ?: return this

    val out = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    bitmap.recycle()
    return out.toByteArray()
}