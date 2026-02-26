package com.mintanable.notethepad.feature_note.domain.use_case

import android.net.Uri
import com.mintanable.notethepad.core.file.FileManager
import javax.inject.Inject

class DeleteFiles @Inject constructor(
    private val fileManager: FileManager
) {

    suspend operator fun invoke(list: List<String>) {
         fileManager.deleteFiles(list)
    }
}