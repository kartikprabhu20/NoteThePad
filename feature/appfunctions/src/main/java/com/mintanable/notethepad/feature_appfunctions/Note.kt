package com.mintanable.notethepad.feature_appfunctions

import androidx.appfunctions.AppFunctionSerializable

/** A note exposed via NoteThePad's AppFunctions surface. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class Note(
    /** The note's identifier. */
    val id: String,
    /** The note's title. */
    val title: String,
    /** The note's content. */
    val content: String,
)
