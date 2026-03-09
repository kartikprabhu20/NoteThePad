package com.mintanable.notethepad.feature_navigationdrawer.domain.repository

import com.mintanable.notethepad.feature_navigationdrawer.domain.model.DrawerItem
import kotlinx.coroutines.flow.Flow

interface NavigationDrawerItemRepository {
    fun getNavigationDrawerItems(): Flow<List<DrawerItem>>
}