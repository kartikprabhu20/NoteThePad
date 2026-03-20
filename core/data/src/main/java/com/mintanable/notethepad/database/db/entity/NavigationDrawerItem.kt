package com.mintanable.notethepad.database.db.entity

import androidx.compose.ui.graphics.vector.ImageVector

sealed class DrawerItem {
    data class NavigationDrawerItem(val title: String, val icon: ImageVector, val route: String): DrawerItem()
    data class TextDrawerItem(val title: String): DrawerItem()
    data class LabelDrawerItem(val tagEntity: TagEntity, val icon: ImageVector, val route: String): DrawerItem()
    data class AddLabelDrawerItem(val title: String, val icon: ImageVector, val route: String): DrawerItem()
}
