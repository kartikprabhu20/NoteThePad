package com.mintanable.notethepad.feature_note.domain.use_case

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.mintanable.notethepad.core.file.FileManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject

class CreateTempFile @Inject constructor(
    private val fileManager: FileManager
) {

     operator fun invoke(extension: String): File? {
        return fileManager.createTempFile(extension)
    }
}