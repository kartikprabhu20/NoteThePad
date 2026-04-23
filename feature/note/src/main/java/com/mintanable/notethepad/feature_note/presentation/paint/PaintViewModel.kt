package com.mintanable.notethepad.feature_note.presentation.paint

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.mintanable.notethepad.file.FileManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PaintViewModel @Inject constructor(
    private val fileManager: FileManager
) : ViewModel() {

    suspend fun saveBitmap(bitmap: Bitmap, existingPath: String?): String? = withContext(Dispatchers.IO) {
        val destFile = if (!existingPath.isNullOrBlank()) {
            File(existingPath)
        } else {
            File(
                fileManager.getMediaDir(),
                "paint_${System.currentTimeMillis()}_${UUID.randomUUID()}.png"
            )
        }
        runCatching {
            FileOutputStream(destFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            destFile.absolutePath
        }.getOrNull()
    }
}
