package com.mintanable.notethepad.feature_navigationdrawer.domain.model

import androidx.compose.ui.graphics.vector.ImageVector

sealed class DrawerItem {
    data class NavigationDrawerItem(val title: String, val icon: ImageVector, val route: String): DrawerItem()
    data class TextDrawerItem(val title: String): DrawerItem()
    data class LabelDrawerItem(val title: String, val icon: ImageVector, val route: String): DrawerItem()
    data class AddLabelDrawerItem(val title: String, val icon: ImageVector, val route: String): DrawerItem()
}
