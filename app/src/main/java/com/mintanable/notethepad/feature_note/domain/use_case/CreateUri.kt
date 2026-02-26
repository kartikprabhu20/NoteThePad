package com.mintanable.notethepad.feature_note.domain.use_case

import android.net.Uri
import com.mintanable.notethepad.core.file.FileManager
import javax.inject.Inject

class CreateUri @Inject constructor(
    private val fileManager: FileManager
) {

     operator fun invoke(extension: String, prefix: String?): Uri? {
        return fileManager.createUri(extension, prefix)
    }
}