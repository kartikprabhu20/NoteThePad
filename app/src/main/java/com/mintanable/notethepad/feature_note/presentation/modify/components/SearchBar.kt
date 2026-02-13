package com.mintanable.notethepad.feature_note.presentation.modify.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SearchBar(
    text: String,
    onValueChange: (String)-> Unit,
    onFocusChanged: (FocusState)-> Unit,
    onClearClicked: () -> Unit,
    hint: String = "Search notes...",
    modifier: Modifier=Modifier,
    textStyle: TextStyle = TextStyle(),
    isSingleLine: Boolean = false
) {
    var rotationAngle by remember { mutableStateOf(0f) }
    val rotation by animateFloatAsState(
        targetValue = rotationAngle,
        animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing),
        label = "ClearIconRotation"
    )
    val scope = rememberCoroutineScope()

    TextField(
        value = text,
        onValueChange = onValueChange,
        modifier = modifier
            .onFocusChanged { focusState ->
                onFocusChanged(focusState)
            }
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null
            )
        },
        trailingIcon = {
            AnimatedVisibility(
                visible = text.isNotBlank(),
                enter = fadeIn() + scaleIn(),
                exit = fadeOut(animationSpec = tween(durationMillis = 400)) + scaleOut()
            ) {
                IconButton(
                    onClick = {
                        rotationAngle += 360f
                        scope.launch {
                            delay(200L) // Wait for the rotation to finish
                            onClearClicked()
                        }
                              },
                    modifier = Modifier.rotate(rotation)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Clear text"
                    )
                }
            }
        },
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
            focusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
        ),
        placeholder = {
            Text(hint)
        },
        singleLine = isSingleLine,
        textStyle = textStyle
    )
}

@Preview
@Composable
fun PreviewSearchBar(modifier: Modifier = Modifier) {
    MaterialTheme {
        var searchQuery by  remember{ mutableStateOf("")}
        SearchBar(searchQuery,onValueChange = {searchQuery=it}, onFocusChanged = {}, onClearClicked = {searchQuery=""})
    }
}