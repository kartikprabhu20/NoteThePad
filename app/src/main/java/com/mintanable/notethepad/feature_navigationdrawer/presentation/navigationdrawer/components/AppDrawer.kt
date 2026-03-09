package com.mintanable.notethepad.feature_navigationdrawer.presentation.navigationdrawer.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.mintanable.notethepad.BuildConfig
import com.mintanable.notethepad.feature_firebase.domain.model.User
import com.mintanable.notethepad.feature_navigationdrawer.domain.model.DrawerItem
import com.mintanable.notethepad.feature_note.domain.model.Tag
import com.mintanable.notethepad.ui.theme.NoteThePadTheme
import com.mintanable.notethepad.ui.theme.ThemePreviews
import com.mintanable.notethepad.ui.util.Screen

@Composable
fun AppDrawer(
    user: User?,
    items: List<DrawerItem>,
    selectedItemIndex: Int,
    modifier: Modifier = Modifier,
    onItemSelected: (Int, DrawerItem) -> Unit
) {
    ModalDrawerSheet(modifier = modifier) {

        Column(modifier = Modifier.fillMaxHeight()) {
            DrawerHeader(user = user, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                itemsIndexed(
                    items = items,
                    key = { index, item ->
                        when(item) {
                            is DrawerItem.NavigationDrawerItem -> item.route
                            is DrawerItem.TextDrawerItem -> "header_${item.title}"
                            is DrawerItem.AddLabelDrawerItem -> "add_label"
                            is DrawerItem.LabelDrawerItem -> item.route
                        }
                    }
                ) { index, item ->

                    when(item){
                        is DrawerItem.NavigationDrawerItem -> {
                            NavigationDrawerItem(
                                label = { Text(text = item.title) },
                                selected = index == selectedItemIndex,
                                onClick = {
                                    onItemSelected(index, item)
                                },
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.title
                                    )
                                },
                                modifier = Modifier
                                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                        }

                        is DrawerItem.LabelDrawerItem -> {
                            NavigationDrawerItem(
                                label = { Text(text = item.tag.tagName) },
                                selected = index == selectedItemIndex,
                                onClick = {
                                    onItemSelected(index, item)
                                },
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.tag.tagName
                                    )
                                },
                                modifier = Modifier
                                    .padding(start = 16.dp)
                                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                            )

                        }

                        is DrawerItem.TextDrawerItem -> {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    item.title,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 32.dp)
                                        .padding(vertical = 8.dp)
                                )
                                IconButton(
                                    onClick = {}
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "edit labels"
                                    )
                                }
                            }

                        }

                        is DrawerItem.AddLabelDrawerItem -> {
                            NavigationDrawerItem(
                                label = { Text(text = item.title) },
                                selected = index == selectedItemIndex,
                                onClick = {
                                    onItemSelected(index, item)
                                },
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.title
                                    )
                                },
                                modifier = Modifier
                                    .padding(start = 16.dp)
                                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                            )

                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                        }
                    }


                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                Text(
                    text = "Version ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Build ${BuildConfig.VERSION_CODE}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun DrawerHeader(
    modifier: Modifier = Modifier,
    title: String = "NoteThePad",
    user: User?
) {
    Column(modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            modifier = Modifier.padding(16.dp), // Use fixed modifiers inside
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        user?.photoUrl?.let { url ->
            if (url.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(url)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .size(72.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }

        user?.displayName?.let { name ->
            if (name.isNotBlank()) {
                Text(
                    text = name,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    }
}

@ThemePreviews
@Composable
fun PreviewDrawHeader(){
    NoteThePadTheme {
        DrawerHeader(
            Modifier,
            "NoteThePad",
            User(
                "1",
                "test@gmail.com",
                "testUser",
                "testuri",
                false))
    }
}

@ThemePreviews
@Composable
fun PreviewAppDrawer() {
    val isLoggedIn = false
    val items = listOf(
        DrawerItem.NavigationDrawerItem(
            title = "Home",
            icon = Icons.Filled.Home,
            route = Screen.NotesScreen.route
        ),
        DrawerItem.NavigationDrawerItem(
            title = "Reminders",
            icon = Icons.Filled.Notifications,
            route = Screen.RemindersScreen.route
        ),
        DrawerItem.TextDrawerItem(
            title = "Labels"
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
            title = "Logout",
            icon = Icons.AutoMirrored.Filled.Logout,
            route = Screen.LogOut.route
        )
    )
    val tags = listOf(Tag("Home"), Tag("Work"), Tag("Shopping"))
    val resultList = mutableListOf<DrawerItem>()
    items.forEach { item ->
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

    NoteThePadTheme {
        AppDrawer(
            user = null,
            items = resultList,
            selectedItemIndex = 1,
            onItemSelected = {_,_ -> },
        )
    }
}