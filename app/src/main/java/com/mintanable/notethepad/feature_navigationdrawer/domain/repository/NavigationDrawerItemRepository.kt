package com.mintanable.notethepad.feature_navigationdrawer.domain.repository

import com.mintanable.notethepad.feature_navigationdrawer.domain.model.NavigationDrawerItem
import kotlinx.coroutines.flow.Flow

interface NavigationDrawerItemRepository {
    fun getNavigationDrawerItems(): Flow<List<NavigationDrawerItem>>
}