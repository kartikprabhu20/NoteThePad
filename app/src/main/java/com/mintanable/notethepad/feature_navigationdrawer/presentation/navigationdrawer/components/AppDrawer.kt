package com.mintanable.notethepad.feature_navigationdrawer.presentation.navigationdrawer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.feature_navigationdrawer.domain.model.NavigationDrawerItem

@Composable
fun AppDrawer(
    items: List<NavigationDrawerItem>,
    selectedItemIndex: Int,
    modifier: Modifier = Modifier,
    onItemSelected: (Int, NavigationDrawerItem) -> Unit
) {
    ModalDrawerSheet(modifier = Modifier) {
        DrawerHeader(modifier = modifier)
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
fun DrawerHeader(title: String ="NoteThePad",  modifier: Modifier) {
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = title,
        modifier = modifier
            .padding(16.dp),
        style = MaterialTheme.typography.titleLarge
    )
    HorizontalDivider()
}
