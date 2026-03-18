package com.mintanable.notethepad.feature_settings.presentation.components

import android.content.res.Configuration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.mintanable.notethepad.feature_settings.R
import com.mintanable.notethepad.feature_settings.presentation.util.PermissionRationaleType
import com.mintanable.notethepad.theme.NoteThePadTheme

@Composable
fun PermissionRationaleDialog(
    permissionRationaleType: PermissionRationaleType,
    onConfirmClicked: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(permissionRationaleType.titleRes)) },
        text = {
            Text(stringResource(permissionRationaleType.messageRes))
        },
        confirmButton = {
            TextButton(onClick = onConfirmClicked) {
                Text(stringResource(R.string.btn_allow))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.btn_not_now))
            }
        }
    )
}


@Preview(name = "Light Mode", showBackground = true)
@Preview(
    name = "Dark Mode",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewPermissionRationaleDialog(){
    NoteThePadTheme {
        PermissionRationaleDialog(PermissionRationaleType.NOTIFICATION, {}, {})
    }
}