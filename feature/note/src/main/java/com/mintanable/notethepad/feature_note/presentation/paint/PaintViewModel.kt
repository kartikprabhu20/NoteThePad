package com.mintanable.notethepad.feature_note.presentation.paint

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.mintanable.notethepad.file.FileManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class PaintViewModel @Inject constructor(
    private val fileManager: FileManager,
    private val snapshotTracker: PaintSnapshotTracker
) : ViewModel() {

    val canUndo: StateFlow<Boolean> = snapshotTracker.canUndo
    val canRedo: StateFlow<Boolean> = snapshotTracker.canRedo

    fun pushSnapshot(bitmap: Bitmap) {
        bitmap.config?.let {
            val copy = bitmap.copy(it, true)
            snapshotTracker.applySnapshot(PaintSnapshot(copy))
        }
    }

    fun undo(): Bitmap? {
        return snapshotTracker.undo()?.bitmap?.copy(Bitmap.Config.ARGB_8888, true)
    }

    fun redo(): Bitmap? {
        return snapshotTracker.redo()?.bitmap?.copy(Bitmap.Config.ARGB_8888, true)
    }

    suspend fun saveBitmap(bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        val destFile = File(
            fileManager.getMediaDir(),
            "paint_${System.currentTimeMillis()}_${UUID.randomUUID()}.png"
        )
        runCatching {
            FileOutputStream(destFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Uri.fromFile(destFile).toString()
        }.getOrNull()
    }
}
