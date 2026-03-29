package com.mintanable.notethepad.core.richtext.engine

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.mintanable.notethepad.core.richtext.compose.RichTextAnnotator
import com.mintanable.notethepad.core.richtext.model.RichTextDocument
import com.mintanable.notethepad.core.richtext.model.RichTextState
import com.mintanable.notethepad.core.richtext.model.SpanType
import com.mintanable.notethepad.core.richtext.utils.TextUtils

object RichTextEngine {

    /**
     * Handles text input changes and maintains rich text state.
     */
    fun onValueChanged(
        oldState: RichTextState,
        newValue: TextFieldValue
    ): RichTextState {
        val oldDoc = oldState.document
        val oldText = oldState.document.rawText
        val newText = newValue.text
        val newCursorPos = newValue.selection.start

        if (newText == oldText) {
            // Cursor/selection moved without text change
            return deriveStateFromCursor(oldDoc, newValue)
        }

        val changeStart = TextUtils.findTextChangeStart(oldText, newText)
        val changeEnd = TextUtils.findTextChangeEnd(oldText, newText, changeStart)
        var newDoc = SpanAdjustmentEngine.adjustForTextChange(oldDoc, newText, changeStart, changeEnd)

        val isInsertion = newText.length > oldText.length
        var adjustedCursorPos = newCursorPos

        if (isInsertion) {
            val insertLen = newText.length - oldText.length
            val insertEnd = changeStart + insertLen
            val insertedText = newText.substring(changeStart, insertEnd)

            // Bullet continuation on Enter
            if (oldState.pendingBullet && '\n' in insertedText) {
                var tempDoc = newDoc
                var cursorOffset = 0
                
                for (i in insertedText.indices) {
                    if (insertedText[i] == '\n') {
                        val newlinePos = changeStart + i + cursorOffset
                        val (bulletDoc, extra) = SpanAdjustmentEngine.continueBulletAfterEnter(tempDoc, newlinePos)
                        tempDoc = bulletDoc
                        cursorOffset += extra
                    }
                }
                newDoc = tempDoc
                adjustedCursorPos += cursorOffset
            }

            // Ensure BULLET span covers the line(s) if pendingBullet is active
            if (oldState.pendingBullet) {
                newDoc = SpanAdjustmentEngine.ensureBullet(newDoc, changeStart, changeStart + 1)
                if ('\n' in insertedText) {
                    val allLineRanges = SpanAdjustmentEngine.getLineRanges(newDoc.rawText, changeStart, adjustedCursorPos)
                    for ((ls, le) in allLineRanges) {
                        if (le > ls) {
                            newDoc = SpanAdjustmentEngine.ensureBullet(newDoc, ls, le)
                        }
                    }
                }
            }

            // Ensure pending block type covers the line(s) containing the insertion
            if (oldState.pendingBlockType != null) {
                newDoc = SpanAdjustmentEngine.ensureBlockType(newDoc, oldState.pendingBlockType, changeStart, changeStart + 1)
                if ('\n' in insertedText) {
                    val allLineRanges = SpanAdjustmentEngine.getLineRanges(newDoc.rawText, changeStart, adjustedCursorPos)
                    for ((ls, le) in allLineRanges) {
                        if (le > ls) {
                            newDoc = SpanAdjustmentEngine.ensureBlockType(newDoc, oldState.pendingBlockType, ls, le)
                        }
                    }
                }
            }

            // Apply pending inline styles to the user's inserted chars
            for (style in oldState.pendingStyles) {
                if (!newDoc.isActiveAt(style, changeStart, insertEnd)) {
                    newDoc = SpanAdjustmentEngine.toggleSpan(newDoc, style, changeStart, insertEnd)
                }
            }
            // Remove inline styles that are active on inserted chars but NOT in pendingStyles
            val inlineTypes = listOf(SpanType.BOLD, SpanType.ITALIC, SpanType.UNDERLINE)
            for (style in inlineTypes) {
                if (style !in oldState.pendingStyles && newDoc.isActiveAt(style, changeStart, insertEnd)) {
                    newDoc = SpanAdjustmentEngine.toggleSpan(newDoc, style, changeStart, insertEnd)
                }
            }
        }

        val annotated = RichTextAnnotator.toAnnotatedString(newDoc)
        val finalCursor = adjustedCursorPos.coerceIn(0, newDoc.rawText.length)
        val finalSelection = if (newValue.selection.start != newValue.selection.end) {
             TextRange(finalCursor, (newValue.selection.end + (adjustedCursorPos - newCursorPos)).coerceIn(0, newDoc.rawText.length))
        } else {
             TextRange(finalCursor)
        }

        val newTFV = TextFieldValue(annotatedString = annotated, selection = finalSelection)

        return if (!isInsertion) {
            deriveStateFromCursor(newDoc, newTFV)
        } else {
            oldState.copy(
                document = newDoc,
                textFieldValue = newTFV
            )
        }
    }

    /**
     * Toggles a format type on the current selection.
     */
    fun toggleFormat(state: RichTextState, type: SpanType): RichTextState {
        val sel = state.textFieldValue.selection

        return when (type) {
            SpanType.BULLET -> toggleBullet(state)
            in SpanAdjustmentEngine.BLOCK_TYPES -> toggleBlockType(state, type)
            else -> toggleInlineStyle(state, type)
        }
    }

    private fun toggleBullet(state: RichTextState): RichTextState {
        val doc = state.document
        val sel = state.textFieldValue.selection
        val hasSelection = sel.start < sel.end
        val lineRanges = SpanAdjustmentEngine.getLineRanges(doc.rawText, sel.start, sel.end)
        val isEmptyLine = doc.rawText.isEmpty() || lineRanges.all { (ls, le) -> ls == le }

        val currentlyBullet = lineRanges.any { (ls, le) ->
            le > ls && doc.isActiveAt(SpanType.BULLET, ls, le)
        }
        val newBulletIntent = if (isEmptyLine) !state.pendingBullet else !currentlyBullet

        var newDoc = doc
        var cursorDelta = 0
        val cursorPos = sel.start
        val lineStart = lineRanges.first().first

        if (newBulletIntent) {
            val (prefixInserted, delta) = SpanAdjustmentEngine.insertBulletPrefixes(newDoc, lineRanges, cursorPos)
            newDoc = prefixInserted
            cursorDelta += delta

            val updatedLines = SpanAdjustmentEngine.getLineRanges(
                newDoc.rawText, lineStart, (cursorPos + cursorDelta).coerceAtLeast(lineStart + 1)
            )
            val uStart = updatedLines.first().first
            val uEnd = updatedLines.last().second
            if (uEnd > uStart) {
                newDoc = SpanAdjustmentEngine.ensureBullet(newDoc, uStart, uEnd)
                if (state.pendingBlockType != null) {
                    newDoc = SpanAdjustmentEngine.ensureBlockType(newDoc, state.pendingBlockType, uStart, uEnd)
                }
            }
        } else {
            if (!isEmptyLine) {
                val expandedStart = lineRanges.first().first
                val expandedEnd = lineRanges.last().second
                val spans = SpanAdjustmentEngine.removeSpanFromRange(newDoc.spans, SpanType.BULLET, expandedStart, expandedEnd)
                newDoc = RichTextDocument.of(newDoc.rawText, spans)
            }
            val (prefixRemoved, delta) = SpanAdjustmentEngine.removeBulletPrefixes(newDoc, lineRanges, cursorPos)
            newDoc = prefixRemoved
            cursorDelta += delta
        }

        val annotated = RichTextAnnotator.toAnnotatedString(newDoc)
        val newCursorPos = (cursorPos + cursorDelta).coerceIn(0, newDoc.rawText.length)
        val newSelEnd = if (hasSelection) (sel.end + cursorDelta).coerceIn(0, newDoc.rawText.length) else newCursorPos

        return state.copy(
            document = newDoc,
            textFieldValue = TextFieldValue(annotatedString = annotated, selection = TextRange(newCursorPos, newSelEnd)),
            pendingBullet = newBulletIntent
        )
    }

    private fun toggleBlockType(state: RichTextState, type: SpanType): RichTextState {
        val doc = state.document
        val sel = state.textFieldValue.selection
        val lineRanges = SpanAdjustmentEngine.getLineRanges(doc.rawText, sel.start, sel.end)
        val isEmptyLine = doc.rawText.isEmpty() || lineRanges.all { (ls, le) -> ls == le }

        if (isEmptyLine) {
            val newBlock = if (type == state.pendingBlockType) null else type
            return state.copy(pendingBlockType = newBlock)
        } else {
            val newDoc = SpanAdjustmentEngine.applyBlockTypeToLines(doc, type, sel.start, sel.end)
            val annotated = RichTextAnnotator.toAnnotatedString(newDoc)
            return deriveStateFromCursor(newDoc, TextFieldValue(annotatedString = annotated, selection = sel))
        }
    }

    private fun toggleInlineStyle(state: RichTextState, type: SpanType): RichTextState {
        val sel = state.textFieldValue.selection
        if (sel.start < sel.end) {
            val newDoc = SpanAdjustmentEngine.toggleSpan(state.document, type, sel.start, sel.end)
            val annotated = RichTextAnnotator.toAnnotatedString(newDoc)
            return deriveStateFromCursor(newDoc, state.textFieldValue.copy(annotatedString = annotated))
        } else {
            val newInline = if (type in state.pendingStyles) state.pendingStyles - type else state.pendingStyles + type
            return state.copy(pendingStyles = newInline)
        }
    }

    private fun deriveStateFromCursor(doc: RichTextDocument, tfv: TextFieldValue): RichTextState {
        val cursor = tfv.selection.start
        // Inline styles: char left of cursor (empty set when cursor is at position 0)
        val inlineTypes = if (cursor > 0) {
            doc.activeTypesAt(cursor - 1)
                .filter { it !in SpanAdjustmentEngine.BLOCK_TYPES && it != SpanType.BULLET }
                .toSet()
        } else {
            emptySet()
        }

        // Block/bullet: current line spans
        val lineRanges = SpanAdjustmentEngine.getLineRanges(doc.rawText, cursor, cursor)
        val lineStart = lineRanges.firstOrNull()?.first ?: 0
        val lineEnd = lineRanges.lastOrNull()?.second ?: 0

        val block: SpanType?
        val bullet: Boolean
        if (lineEnd > lineStart) {
            val lineTypes = doc.activeTypesAt(lineStart)
            block = lineTypes.firstOrNull { it in SpanAdjustmentEngine.BLOCK_TYPES }
            bullet = SpanType.BULLET in lineTypes
        } else {
            // Empty line: inherit block/bullet from the previous line
            val prevLineBlock = findPreviousLineBlockAndBullet(doc, cursor)
            block = prevLineBlock.first
            bullet = prevLineBlock.second
        }

        // Rebuild the TextFieldValue with the annotated string from the document,
        // preserving the selection/composition from the incoming value.
        // This is critical because Compose's BasicTextField passes plain-text
        // TextFieldValues in onValueChange — without this, styles would be lost
        // on focus gain or cursor movement.
        val annotated = RichTextAnnotator.toAnnotatedString(doc)
        val rebuiltTfv = TextFieldValue(
            annotatedString = annotated,
            selection = tfv.selection,
            composition = tfv.composition
        )

        return RichTextState(
            document = doc,
            textFieldValue = rebuiltTfv,
            pendingBlockType = block,
            pendingBullet = bullet,
            pendingStyles = inlineTypes
        )
    }

    /**
     * Finds the block type and bullet state of the line immediately before [cursor].
     * Returns (blockType, isBullet). If there is no previous line, returns (null, false).
     */
    private fun findPreviousLineBlockAndBullet(doc: RichTextDocument, cursor: Int): Pair<SpanType?, Boolean> {
        if (cursor == 0 || doc.rawText.isEmpty()) return null to false
        // The previous line ends just before the current line starts.
        // Look at the character before cursor (which should be '\n') and find its line.
        val prevCursor = (cursor - 1).coerceAtLeast(0)
        val prevLineRanges = SpanAdjustmentEngine.getLineRanges(doc.rawText, prevCursor, prevCursor)
        val prevStart = prevLineRanges.firstOrNull()?.first ?: return null to false
        val prevEnd = prevLineRanges.lastOrNull()?.second ?: return null to false
        if (prevEnd <= prevStart) return null to false
        val prevTypes = doc.activeTypesAt(prevStart)
        val block = prevTypes.firstOrNull { it in SpanAdjustmentEngine.BLOCK_TYPES }
        val bullet = SpanType.BULLET in prevTypes
        return block to bullet
    }
}
