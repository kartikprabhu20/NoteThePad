package com.mintanable.notethepad.feature_note.domain.use_case

data class FileIOUseCases(
    val createTempFile: CreateTempFile,
    val createTempUri: CreateTempUri,
    val deleteFiles: DeleteFiles,
    val saveMediaToStorage: SaveMediaToStorage
)
