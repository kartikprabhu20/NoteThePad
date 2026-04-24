package com.mintanable.notethepad.feature_note.presentation.modify

import com.mintanable.notethepad.feature_note.presentation.EditNoteSnapshot
import javax.inject.Inject

class SnapshotTracker @Inject constructor() : BaseSnapshotTracker<EditNoteSnapshot>(limit = 50)