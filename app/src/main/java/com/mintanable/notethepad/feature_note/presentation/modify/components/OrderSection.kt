package com.mintanable.notethepad.features.presentation.modify.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.features.domain.util.NoteOrder
import com.mintanable.notethepad.features.domain.util.OrderType

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
                text = "Title",
                selected = noteOrder is NoteOrder.Title,
                onSelect = {onOrderChange(NoteOrder.Title(noteOrder.orderType))}
            )
            Spacer(modifier = Modifier.width(8.dp))
            DefaultRadioButton(
                text = "Date",
                selected = noteOrder is NoteOrder.Date,
                onSelect = {onOrderChange(NoteOrder.Date(noteOrder.orderType))}
            )
            Spacer(modifier = Modifier.width(8.dp))
            DefaultRadioButton(
                text = "Color",
                selected = noteOrder is NoteOrder.Color,
                onSelect = {onOrderChange(NoteOrder.Color(noteOrder.orderType))}
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            DefaultRadioButton(
                text = "Ascending",
                selected = noteOrder.orderType is OrderType.Ascending,
                onSelect = {onOrderChange(noteOrder.copy(OrderType.Ascending))}
            )
            Spacer(modifier = Modifier.width(8.dp))
            DefaultRadioButton(
                text = "Descending",
                selected = noteOrder.orderType is OrderType.Descending,
                onSelect = {onOrderChange(noteOrder.copy(OrderType.Descending))}
            )
        }
    }
}


@Preview(showBackground = true, name = "Order_Section States")
@Composable
fun DefaultOrderSectionPreview() {
    MaterialTheme {
        OrderSection(
            onOrderChange = { }
        )
    }
}