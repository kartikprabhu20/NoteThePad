package com.mintanable.notethepad.feature_navigationdrawer.domain.usecase

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.Add
import com.mintanable.notethepad.feature_navigationdrawer.domain.model.DrawerItem
import com.mintanable.notethepad.feature_navigationdrawer.domain.repository.NavigationDrawerItemRepository
import com.mintanable.notethepad.feature_note.domain.repository.NoteRepository
import com.mintanable.notethepad.ui.util.Screen
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
                                    title = tag.tagName,
                                    icon = Icons.AutoMirrored.Outlined.Label,
                                    route = Screen.LabelsScreen.route + "?label=${tag.tagName}"
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