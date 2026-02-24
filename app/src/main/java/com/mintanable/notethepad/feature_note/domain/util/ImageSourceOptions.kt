package com.mintanable.notethepad.feature_note.domain.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.ui.graphics.vector.ImageVector

enum class ImageSourceOptions(
    override val title: String,
    override val  icon: ImageVector
) : AdditionalOption {
    CAMERA("Camera", Icons.Default.PhotoCamera),
    PHOTO_GALLERY("Gallery", Icons.Default.Collections)
}