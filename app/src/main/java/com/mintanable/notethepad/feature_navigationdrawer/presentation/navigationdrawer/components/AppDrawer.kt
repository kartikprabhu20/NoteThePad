package com.mintanable.notethepad.feature_navigationdrawer.presentation.navigationdrawer.components

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.feature_firebase.domain.model.User
import com.mintanable.notethepad.feature_navigationdrawer.domain.model.NavigationDrawerItem
import coil3.compose.AsyncImage
import com.mintanable.notethepad.ui.theme.NoteThePadTheme

@Composable
fun AppDrawer(
    user: User?,
    items: List<NavigationDrawerItem>,
    selectedItemIndex: Int,
    modifier: Modifier = Modifier,
    onItemSelected: (Int, NavigationDrawerItem) -> Unit
) {
    Log.i("kptest", "AppDrawer user: $user")
    Log.i("kptest", "AppDrawer $items")
    ModalDrawerSheet(modifier = Modifier) {
        DrawerHeader(modifier = modifier, user = user)
        Spacer(modifier = Modifier.padding(5.dp))
        items.forEachIndexed { index, item ->
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

@Composable
fun DrawerHeader(title: String ="NoteThePad",
                 user: User?,
                 modifier: Modifier) {

    Column(
        modifier = modifier
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            modifier = modifier
                .padding(16.dp),
            style = MaterialTheme.typography.titleLarge
        )

        if(user?.photoUrl?.isNotBlank() == true){
            AsyncImage(
                model = user.photoUrl,
                contentDescription = "Profile Picture",
                modifier = Modifier.padding(horizontal = 16.dp).size(72.dp).clip(CircleShape),
                placeholder = rememberVectorPainter(Icons.Default.AccountCircle),
                error = rememberVectorPainter(Icons.Default.AccountCircle),
                contentScale = ContentScale.Crop
            )
        }

        if(user?.displayName?.isNotBlank() == true){
            Text(
                text = user.displayName,
                modifier = modifier
                    .padding(16.dp),
                style = MaterialTheme.typography.titleMedium
            )
        }
        HorizontalDivider()
    }
}

@Preview
@Composable
fun previewDrawHeader(){
    NoteThePadTheme {
        DrawerHeader("NoteThePad",
            User("1","test@gmail.com", "testUser","testuri"),
            Modifier)
    }
}
