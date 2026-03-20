package com.mintanable.notethepad.database.db.repository

import com.mintanable.notethepad.core.model.settings.DrawerItem
import kotlinx.coroutines.flow.Flow

interface NavigationDrawerItemRepository {
    fun getNavigationDrawerItems(): Flow<List<DrawerItem>>
}