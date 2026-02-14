package com.mintanable.notethepad.feature_navigationdrawer.domain.model

import androidx.compose.ui.graphics.vector.ImageVector

data class NavigationDrawerItem(
    val title: String,
    val icon: ImageVector,
    val route: String
)
