package com.mintanable.notethepad.feature_settings.presentation.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material3.TextButton
import androidx.compose.ui.tooling.preview.Preview
import com.mintanable.notethepad.feature_settings.presentation.util.PermissionRationaleType
import com.mintanable.notethepad.ui.theme.NoteThePadTheme

@Composable
fun PermissionRationaleDialog(
    permissionRationaleType: PermissionRationaleType,
    onConfirmClicked: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(permissionRationaleType.title) },
        text = {
            Text(permissionRationaleType.message)
        },
        confirmButton = {
            TextButton(onClick = onConfirmClicked) {
                Text("Allow")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Not Now")
            }
        }
    )
}


@Preview(name = "Light Mode", showBackground = true)
@Preview(
    name = "Dark Mode",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewPermissionRationaleDialog(){
    NoteThePadTheme {
        PermissionRationaleDialog(PermissionRationaleType.NOTIFICATION, {}, {})
    }
}