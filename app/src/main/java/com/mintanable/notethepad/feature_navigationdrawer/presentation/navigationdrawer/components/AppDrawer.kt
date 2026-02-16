package com.mintanable.notethepad.feature_navigationdrawer.presentation.navigationdrawer.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.mintanable.notethepad.ui.theme.NoteThePadTheme

@Composable
fun AppDrawer(
    user: User?,
    items: List<NavigationDrawerItem>,
    selectedItemIndex: Int,
    modifier: Modifier = Modifier,
    onItemSelected: (Int, NavigationDrawerItem) -> Unit
) {
    ModalDrawerSheet(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                DrawerHeader(user = user, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
            }

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
    }
}

@Composable
fun DrawerHeader(
    title: String = "NoteThePad",
    user: User?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
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

@Preview(name = "Light Mode", showBackground = true)
@Preview(
    name = "Dark Mode",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun previewDrawHeader(){
    NoteThePadTheme {
        DrawerHeader("NoteThePad",
            User("1","test@gmail.com", "testUser","testuri"),
            Modifier)
    }
}
