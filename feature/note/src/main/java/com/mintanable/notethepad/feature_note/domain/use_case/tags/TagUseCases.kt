package com.mintanable.notethepad.feature_note.domain.use_case.tags

data class TagUseCases(
    val getAllTags: GetAllTags,
    val deleteTag: DeleteTag,
    val saveTag: SaveTag,
)
