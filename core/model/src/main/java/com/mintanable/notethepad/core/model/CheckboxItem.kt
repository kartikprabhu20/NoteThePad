package com.mintanable.notethepad.core.model

import java.util.UUID

data class CheckboxItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isChecked: Boolean = false
)