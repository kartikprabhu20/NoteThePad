package com.mintanable.notethepad.feature_navigationdrawer.data.repository

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import com.mintanable.notethepad.feature_navigationdrawer.domain.model.NavigationDrawerItem
import com.mintanable.notethepad.feature_navigationdrawer.domain.repository.NavigationDrawerItemRepository
import com.mintanable.notethepad.ui.util.Screen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class NavigationDrawerItemRepositoryImpl: NavigationDrawerItemRepository {

    override fun getNavigationDrawerItems(): Flow<List<NavigationDrawerItem>> {
        val items = listOf(
            NavigationDrawerItem(
                title = "Home",
                icon = Icons.Filled.Home,
                route = Screen.NotesScreen.route
            ),
            NavigationDrawerItem(
                title = "Settings",
                icon = Icons.Filled.Settings,
                route = Screen.NotesScreen.route
            ),
            NavigationDrawerItem(
                title = "Login",
                icon = Icons.AutoMirrored.Filled.Login,
                route = Screen.FirebaseLoginScreen.route
            ),
            NavigationDrawerItem(
                title = "Logout",
                icon = Icons.AutoMirrored.Filled.Logout,
                route = Screen.FirebaseLoginScreen.route
            )
        )
        return flowOf(items)
    }
}