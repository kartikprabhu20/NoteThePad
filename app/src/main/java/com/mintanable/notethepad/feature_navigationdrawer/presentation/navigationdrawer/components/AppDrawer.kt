package com.mintanable.notethepad.feature_navigationdrawer.presentation.navigationdrawer.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.feature_firebase.domain.model.User
import com.mintanable.notethepad.feature_navigationdrawer.domain.model.NavigationDrawerItem
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.google.firebase.auth.GoogleAuthProvider
import com.mintanable.notethepad.BuildConfig
import com.mintanable.notethepad.ui.theme.NoteThePadTheme
import com.mintanable.notethepad.ui.theme.ThemePreviews
import com.mintanable.notethepad.ui.util.Screen

@Composable
fun AppDrawer(
    user: User?,
    items: List<NavigationDrawerItem>,
    selectedItemIndex: Int,
    modifier: Modifier = Modifier,
    onItemSelected: (Int, NavigationDrawerItem) -> Unit
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
                    key = { _, item -> item.route }
                ) { index, item ->
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
    title: String = "NoteThePad",
    user: User?,
    modifier: Modifier = Modifier
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
        DrawerHeader("NoteThePad",
            User(
                "1",
                "test@gmail.com",
                "testUser",
                "testuri",
                false),
            Modifier)
    }
}

@ThemePreviews
@Composable
fun PreviewAppDrawer() {
    NoteThePadTheme {
        AppDrawer(
            user = null,
            items = listOf(
                NavigationDrawerItem(
                    title = "Home",
                    icon = Icons.Filled.Home,
                    route = Screen.NotesScreen.route
                ),
                NavigationDrawerItem(
                    title = "Settings",
                    icon = Icons.Filled.Settings,
                    route = Screen.SettingsScreen.route
                ),
                NavigationDrawerItem(
                    title = "Login",
                    icon = Icons.AutoMirrored.Filled.Login,
                    route = Screen.FirebaseLoginScreen.route
                ),
                NavigationDrawerItem(
                    title = "Logout",
                    icon = Icons.AutoMirrored.Filled.Logout,
                    route = Screen.LogOut.route
                ),

            ),
            selectedItemIndex = 1,
            onItemSelected = {_,_ -> },
        )
    }
}