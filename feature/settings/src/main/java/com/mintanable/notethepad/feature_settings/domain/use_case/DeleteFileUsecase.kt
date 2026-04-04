package com.mintanable.notethepad.feature_settings.domain.use_case

import com.mintanable.notethepad.file.FileManager
import javax.inject.Inject

class DeleteFileUsecase @Inject constructor(
    private val fileManager: FileManager
) {
    suspend operator fun invoke(path: String): Result<Any> {
        return fileManager.deleteFiles(listOf(path))
    }
}