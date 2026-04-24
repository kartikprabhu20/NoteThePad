package com.mintanable.notethepad.feature_note.presentation.paint

import com.mintanable.notethepad.feature_note.presentation.modify.BaseSnapshotTracker
import javax.inject.Inject

class PaintSnapshotTracker @Inject constructor() : BaseSnapshotTracker<PaintSnapshot>(limit = 10)
