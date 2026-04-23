package com.mintanable.notethepad.database.db.repository

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import com.mintanable.notethepad.core.common.NotesFilterType
import com.mintanable.notethepad.core.common.Screen
import com.mintanable.notethepad.database.db.entity.DrawerItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class NavigationDrawerItemRepositoryImpl: NavigationDrawerItemRepository {

    override fun getNavigationDrawerItems(): Flow<List<DrawerItem>> {
        val items = listOf(
            DrawerItem.NavigationDrawerItem(
                title = "Home",
                icon = Icons.Filled.Home,
                route = Screen.NotesScreen.route
            ),
            DrawerItem.NavigationDrawerItem(
                title = "Reminders",
                icon = Icons.Filled.Notifications,
                route = Screen.NotesScreen.passArgs(filterType = NotesFilterType.REMINDERS.filter)
            ),
            DrawerItem.NavigationDrawerItem(
                title = "Shared",
                icon = Icons.Filled.Share,
                route = Screen.NotesScreen.passArgs(filterType = NotesFilterType.SHARED.filter)
            ),
            DrawerItem.NavigationDrawerItem(
                title = "Calendar",
                icon = Icons.Filled.CalendarMonth,
                route = Screen.CalendarScreen.route
            ),
            DrawerItem.TextDrawerItem(
                title = "Labels"
            ),
            DrawerItem.NavigationDrawerItem(
                title = "Archive",
                icon = Icons.Filled.Archive,
                route = Screen.ArchiveScreen.route
            ),
            DrawerItem.NavigationDrawerItem(
                title = "Settings",
                icon = Icons.Filled.Settings,
                route = Screen.SettingsScreen.route
            ),
            DrawerItem.NavigationDrawerItem(
                title = "Login",
                icon = Icons.AutoMirrored.Filled.Login,
                route = Screen.FirebaseLoginScreen.route
            ),
            DrawerItem.NavigationDrawerItem(
                title = "Help and Feedback",
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                route = Screen.HelpAndFeedbackScreen.route
            ),
            DrawerItem.NavigationDrawerItem(
                title = "Logout",
                icon = Icons.AutoMirrored.Filled.Logout,
                route = Screen.LogOut.route
            )
        )
        return flowOf(items)
    }
}