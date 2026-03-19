package com.mintanable.notethepad.feature_note.presentation.notes.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.core.model.NoteOrder
import com.mintanable.notethepad.core.model.OrderType
import com.mintanable.notethepad.feature_note.R
import com.mintanable.notethepad.feature_note.presentation.modify.components.DefaultRadioButton
import com.mintanable.notethepad.theme.NoteThePadTheme

@Composable
fun OrderSection (
    modifier:Modifier = Modifier,
    noteOrder: NoteOrder = NoteOrder.Date(OrderType.Descending),
    onOrderChange: (NoteOrder)->Unit
){
    Column(
        modifier=modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            DefaultRadioButton(
                text = stringResource(R.string.order_title),
                selected = noteOrder is NoteOrder.Title,
                onSelect = {onOrderChange(NoteOrder.Title(noteOrder.orderType))}
            )
            Spacer(modifier = Modifier.width(8.dp))
            DefaultRadioButton(
                text = stringResource(R.string.order_date),
                selected = noteOrder is NoteOrder.Date,
                onSelect = {onOrderChange(NoteOrder.Date(noteOrder.orderType))}
            )
            Spacer(modifier = Modifier.width(8.dp))
            DefaultRadioButton(
                text = stringResource(R.string.order_color),
                selected = noteOrder is NoteOrder.Color,
                onSelect = {onOrderChange(NoteOrder.Color(noteOrder.orderType))}
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            DefaultRadioButton(
                text = stringResource(R.string.order_ascending),
                selected = noteOrder.orderType is OrderType.Ascending,
                onSelect = {onOrderChange(noteOrder.copyOrder(OrderType.Ascending))}
            )
            Spacer(modifier = Modifier.width(8.dp))
            DefaultRadioButton(
                text = stringResource(R.string.order_descending),
                selected = noteOrder.orderType is OrderType.Descending,
                onSelect = {onOrderChange(noteOrder.copyOrder(OrderType.Descending))}
            )
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(
    name = "Dark Mode",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)@Composable
fun DefaultOrderSectionPreview() {
    NoteThePadTheme {
        OrderSection(
            onOrderChange = { }
        )
    }
}