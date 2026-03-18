package com.mintanable.notethepad.feature_note.domain.use_case

data class FileIOUseCases(
    val createFile: CreateFile,
    val createUri: CreateUri,
    val deleteFiles: DeleteFiles,
    val saveMediaToStorage: SaveMediaToStorage
)
