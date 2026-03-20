package com.mintanable.notethepad.core.model.settings

import androidx.compose.ui.graphics.vector.ImageVector
import com.mintanable.notethepad.core.model.note.Tag

sealed class DrawerItem {
    data class NavigationDrawerItem(val title: String, val icon: ImageVector, val route: String): DrawerItem()
    data class TextDrawerItem(val title: String): DrawerItem()
    data class LabelDrawerItem(val tag: Tag, val icon: ImageVector, val route: String): DrawerItem()
    data class AddLabelDrawerItem(val title: String, val icon: ImageVector, val route: String): DrawerItem()
}
