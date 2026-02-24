package com.mintanable.notethepad.feature_note.presentation.modify.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.feature_note.domain.util.AdditionalOption
import com.mintanable.notethepad.feature_note.domain.util.AttachmentOptions
import com.mintanable.notethepad.ui.theme.NoteThePadTheme

@Composable
fun BottomSheetContent(
    items: List<AdditionalOption>,
    optionSelected: (AdditionalOption) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        items.forEachIndexed { index, additionalOption ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { optionSelected(additionalOption)}
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(additionalOption.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(16.dp))
                Text(text = additionalOption.title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            }
            if (index < items.lastIndex) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

        }
    }
}


@Preview(name = "Light Mode", showBackground = true)
@Preview(
    name = "Dark Mode",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewBottomSheetContent(){
    NoteThePadTheme {
        BottomSheetContent(
            items = AttachmentOptions.entries,
            optionSelected = {}
        )
    }
}