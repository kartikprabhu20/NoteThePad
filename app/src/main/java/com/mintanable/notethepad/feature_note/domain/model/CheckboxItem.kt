package com.mintanable.notethepad.feature_note.domain.model

import java.util.UUID

data class CheckboxItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isChecked: Boolean = false
)
