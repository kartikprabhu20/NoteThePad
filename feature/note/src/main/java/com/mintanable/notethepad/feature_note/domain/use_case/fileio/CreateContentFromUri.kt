package com.mintanable.notethepad.feature_note.domain.use_case.fileio

import android.net.Uri
import com.mintanable.notethepad.file.FileManager
import javax.inject.Inject

class CreateContentFromUri @Inject constructor(
    private val fileManager: FileManager
) {
    operator fun invoke(uri: Uri): Uri? {
        return fileManager.uriToContent(uri)
    }
}
