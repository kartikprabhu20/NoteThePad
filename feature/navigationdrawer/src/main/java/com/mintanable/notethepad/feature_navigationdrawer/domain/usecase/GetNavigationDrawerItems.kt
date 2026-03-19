package com.mintanable.notethepad.feature_navigationdrawer.domain.usecase

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.Add
import com.mintanable.notethepad.core.common.NotesFilterType
import com.mintanable.notethepad.core.common.Screen
import com.mintanable.notethepad.database.repository.NoteRepository
import com.mintanable.notethepad.feature_navigationdrawer.domain.model.DrawerItem
import com.mintanable.notethepad.feature_navigationdrawer.domain.repository.NavigationDrawerItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetNavigationDrawerItems(
    private val repository: NavigationDrawerItemRepository,
    private val noteRepository: NoteRepository
) {
    operator fun invoke(isLoggedIn: Boolean = false): Flow<List<DrawerItem>> {

        return combine(
            repository.getNavigationDrawerItems(),
            noteRepository.getAllTags()
        ) { staticItems, tags ->
            val resultList = mutableListOf<DrawerItem>()

            staticItems.forEach { item ->
                val shouldAdd = when (item) {
                    is DrawerItem.NavigationDrawerItem -> {
                        if (item.title == "Login") !isLoggedIn
                        else if (item.title == "Logout") isLoggedIn
                        else true
                    }
                    else -> true
                }

                if (shouldAdd) {
                    resultList.add(item)

                    if (item is DrawerItem.TextDrawerItem && item.title == "Labels") {
                        tags.forEach { tag ->
                            resultList.add(
                                DrawerItem.LabelDrawerItem(
                                    tag = tag,
                                    icon = Icons.AutoMirrored.Outlined.Label,
                                    route = Screen.NotesScreen.passArgs(
                                        tagId = tag.tagId,
                                        tagName = tag.tagName,
                                        filterType = NotesFilterType.TAGS.filter
                                    )
                                )
                            )
                        }
                        resultList.add(
                            DrawerItem.AddLabelDrawerItem(
                                title = "Create new label",
                                icon = Icons.Default.Add,
                                route = "dialog_add_label"
                            )
                        )
                    }
                }
            }
            return@combine resultList
        }
    }
}
