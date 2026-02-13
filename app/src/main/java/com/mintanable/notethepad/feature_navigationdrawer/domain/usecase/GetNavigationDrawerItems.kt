package com.mintanable.notethepad.feature_navigationdrawer.domain.usecase

import com.mintanable.notethepad.feature_navigationdrawer.domain.model.NavigationDrawerItem
import com.mintanable.notethepad.feature_navigationdrawer.domain.repository.NavigationDrawerItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetNavigationDrawerItems(
    private val repository: NavigationDrawerItemRepository
) {
    operator fun invoke(isLoggedIn: Boolean = false): Flow<List<NavigationDrawerItem>> {
        return repository.getNavigationDrawerItems().map { items ->
            items.filter { item ->
                when (item.title) {
                    "Login" -> !isLoggedIn
                    "Logout" -> isLoggedIn
                    else -> true
                }
            }
        }
    }
}