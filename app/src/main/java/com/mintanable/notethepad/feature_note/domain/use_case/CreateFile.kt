package com.mintanable.notethepad.feature_note.domain.use_case

import com.mintanable.notethepad.core.file.FileManager
import java.io.File
import javax.inject.Inject

class CreateFile @Inject constructor(
    private val fileManager: FileManager
) {

     operator fun invoke(extension: String, prefix:String?): File? {
        return fileManager.createFile(extension, prefix)
    }
}