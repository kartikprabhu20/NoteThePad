package com.mintanable.notethepad.feature_navigationdrawer.data.repository

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import com.mintanable.notethepad.feature_navigationdrawer.domain.model.NavigationDrawerItem
import com.mintanable.notethepad.feature_navigationdrawer.domain.repository.NavigationDrawerItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class NavigationDrawerItemRepositoryImpl: NavigationDrawerItemRepository {

    override fun getNavigationDrawerItems(): Flow<List<NavigationDrawerItem>> {
        val items = listOf(
            NavigationDrawerItem(
                title = "Home",
                icon = Icons.Filled.Home,
            ),
            NavigationDrawerItem(
                title = "Settings",
                icon = Icons.Filled.Settings,
            ),
            NavigationDrawerItem(
                title = "Login",
                icon = Icons.AutoMirrored.Filled.Login
            ),
            NavigationDrawerItem(
                title = "Logout",
                icon = Icons.AutoMirrored.Filled.Logout
            )
        )
        return flowOf(items)
    }
}