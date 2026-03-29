package com.mintanable.notethepad.core.richtext.engine

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.google.common.truth.Truth.assertThat
import com.mintanable.notethepad.core.richtext.compose.RichTextAnnotator
import com.mintanable.notethepad.core.richtext.model.RichSpan
import com.mintanable.notethepad.core.richtext.model.RichTextDocument
import com.mintanable.notethepad.core.richtext.model.RichTextState
import com.mintanable.notethepad.core.richtext.model.SpanType
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RichTextEngineTest {

    // ── Helper ──────────────────────────────────────────────────────────

    private fun stateOf(
        text: String,
        spans: List<RichSpan> = emptyList(),
        cursor: Int = text.length,
        pendingStyles: Set<SpanType> = emptySet(),
        pendingBlockType: SpanType? = null,
        pendingBullet: Boolean = false
    ): RichTextState {
        val doc = RichTextDocument.of(text, spans)
        val annotated = RichTextAnnotator.toAnnotatedString(doc)
        return RichTextState(
            document = doc,
            textFieldValue = TextFieldValue(annotatedString = annotated, selection = TextRange(cursor)),
            pendingStyles = pendingStyles,
            pendingBlockType = pendingBlockType,
            pendingBullet = pendingBullet
        )
    }

    private fun tfv(text: String, cursor: Int = text.length) =
        TextFieldValue(text = text, selection = TextRange(cursor))

    // ── onValueChanged: cursor movement (no text change) ────────────────

    @Test
    fun `cursor move derives inline styles from char left of cursor`() {
        val state = stateOf("Hello", spans = listOf(RichSpan(SpanType.BOLD, 0, 3)), cursor = 3)
        // Move cursor to position 2 (inside bold)
        val result = RichTextEngine.onValueChanged(state, state.textFieldValue.copy(selection = TextRange(2)))
        assertThat(result.pendingStyles).contains(SpanType.BOLD)
    }

    @Test
    fun `cursor at position 0 has no inline styles`() {
        val state = stateOf("Hello", spans = listOf(RichSpan(SpanType.BOLD, 0, 5)), cursor = 3)
        val result = RichTextEngine.onValueChanged(state, state.textFieldValue.copy(selection = TextRange(0)))
        assertThat(result.pendingStyles).isEmpty()
    }

    @Test
    fun `cursor move to empty line inherits block and bullet from previous line`() {
        // "Hello\n" with H1 on line 1 and bullet on line 1
        val state = stateOf(
            "Hello\n",
            spans = listOf(
                RichSpan(SpanType.H1, 0, 6),
                RichSpan(SpanType.BULLET, 0, 6)
            ),
            cursor = 3
        )
        // Move cursor to position 6 (empty trailing line)
        val result = RichTextEngine.onValueChanged(state, state.textFieldValue.copy(selection = TextRange(6)))
        assertThat(result.pendingBlockType).isEqualTo(SpanType.H1)
        assertThat(result.pendingBullet).isTrue()
    }

    @Test
    fun `cursor move preserves annotated string styles in textFieldValue`() {
        // This tests the bug where gaining focus (cursor move, no text change)
        // would strip AnnotatedString styles because BasicTextField passes
        // a plain TextFieldValue in onValueChange.
        val state = stateOf(
            "Hello",
            spans = listOf(RichSpan(SpanType.BOLD, 0, 5)),
            cursor = 0
        )
        // Simulate Compose sending a plain TextFieldValue (no AnnotatedString) on focus
        val plainTfv = TextFieldValue(text = "Hello", selection = TextRange(3))
        val result = RichTextEngine.onValueChanged(state, plainTfv)

        // The resulting textFieldValue must still have the bold annotation
        assertThat(result.textFieldValue.annotatedString.spanStyles).isNotEmpty()
        assertThat(result.textFieldValue.annotatedString.spanStyles[0].item.fontWeight)
            .isEqualTo(androidx.compose.ui.text.font.FontWeight.Bold)
    }

    @Test
    fun `cursor on non-empty line reads block type from line spans`() {
        val state = stateOf(
            "Hello\nWorld",
            spans = listOf(RichSpan(SpanType.H2, 6, 11)),
            cursor = 0
        )
        val result = RichTextEngine.onValueChanged(state, state.textFieldValue.copy(selection = TextRange(8)))
        assertThat(result.pendingBlockType).isEqualTo(SpanType.H2)
    }

    // ── onValueChanged: text insertion ──────────────────────────────────

    @Test
    fun `typing character applies pending inline styles`() {
        val state = stateOf("Hello", pendingStyles = setOf(SpanType.BOLD), cursor = 5)
        val result = RichTextEngine.onValueChanged(state, tfv("Hello!", 6))
        assertThat(result.document.isActiveAt(SpanType.BOLD, 5, 6)).isTrue()
    }

    @Test
    fun `typing character applies pending block type`() {
        val state = stateOf("", pendingBlockType = SpanType.H1, cursor = 0)
        val result = RichTextEngine.onValueChanged(state, tfv("H", 1))
        assertThat(result.document.isActiveAt(SpanType.H1, 0, 1)).isTrue()
    }

    @Test
    fun `typing does not apply inline style that was not pending`() {
        // Bold is active on "Hello" but cursor moves past it; pending should not include bold
        val state = stateOf(
            "Hello World",
            spans = listOf(RichSpan(SpanType.BOLD, 0, 5)),
            cursor = 8,
            pendingStyles = emptySet()
        )
        val result = RichTextEngine.onValueChanged(state, tfv("Hello WoXrld", 9))
        assertThat(result.document.isActiveAt(SpanType.BOLD, 8, 9)).isFalse()
    }

    @Test
    fun `typing removes inline style active on inserted range but not in pendingStyles`() {
        // Entire text is bold, but pendingStyles is empty (user toggled bold off)
        val state = stateOf(
            "Hello",
            spans = listOf(RichSpan(SpanType.BOLD, 0, 5)),
            cursor = 5,
            pendingStyles = emptySet()
        )
        val result = RichTextEngine.onValueChanged(state, tfv("Hello!", 6))
        assertThat(result.document.isActiveAt(SpanType.BOLD, 5, 6)).isFalse()
    }

    @Test
    fun `newline with pendingBullet continues bullet on new line`() {
        val state = stateOf(
            "• Hello",
            spans = listOf(RichSpan(SpanType.BULLET, 0, 7)),
            cursor = 7,
            pendingBullet = true
        )
        val result = RichTextEngine.onValueChanged(state, tfv("• Hello\n", 8))
        // Should have inserted "• " after the newline
        assertThat(result.document.rawText).startsWith("• Hello\n• ")
    }

    // ── onValueChanged: text deletion ───────────────────────────────────

    @Test
    fun `deleting text re-derives state from cursor`() {
        val state = stateOf(
            "Hello World",
            spans = listOf(RichSpan(SpanType.BOLD, 0, 5)),
            cursor = 11
        )
        // Delete "World" -> "Hello "
        val result = RichTextEngine.onValueChanged(state, tfv("Hello ", 6))
        // Cursor at 6, char at 5 is space which is inside bold (0-5 exclusive, so char 4 is last bold)
        // Actually bold is 0-5 exclusive, so positions 0,1,2,3,4 are bold. Char at 5 (space) is not bold.
        assertThat(result.pendingStyles).doesNotContain(SpanType.BOLD)
    }

    // ── toggleFormat: inline styles ─────────────────────────────────────

    @Test
    fun `toggleFormat bold with selection applies bold to selection`() {
        val state = stateOf("Hello", cursor = 0).copy(
            textFieldValue = TextFieldValue(text = "Hello", selection = TextRange(0, 5))
        )
        val result = RichTextEngine.toggleFormat(state, SpanType.BOLD)
        assertThat(result.document.isActiveAt(SpanType.BOLD, 0, 5)).isTrue()
    }

    @Test
    fun `toggleFormat bold with selection removes bold when fully active`() {
        val state = stateOf("Hello", spans = listOf(RichSpan(SpanType.BOLD, 0, 5))).copy(
            textFieldValue = TextFieldValue(text = "Hello", selection = TextRange(0, 5))
        )
        val result = RichTextEngine.toggleFormat(state, SpanType.BOLD)
        assertThat(result.document.isActiveAt(SpanType.BOLD, 0, 5)).isFalse()
    }

    @Test
    fun `toggleFormat bold without selection toggles pending style`() {
        val state = stateOf("Hello", cursor = 3)
        val result = RichTextEngine.toggleFormat(state, SpanType.BOLD)
        assertThat(result.pendingStyles).contains(SpanType.BOLD)

        val result2 = RichTextEngine.toggleFormat(result, SpanType.BOLD)
        assertThat(result2.pendingStyles).doesNotContain(SpanType.BOLD)
    }

    // ── toggleFormat: block types ───────────────────────────────────────

    @Test
    fun `toggleFormat H1 on non-empty line applies H1`() {
        val state = stateOf("Hello", cursor = 3)
        val result = RichTextEngine.toggleFormat(state, SpanType.H1)
        assertThat(result.document.isActiveAt(SpanType.H1, 0, 5)).isTrue()
    }

    @Test
    fun `toggleFormat H1 on empty doc sets pending block type`() {
        val state = stateOf("", cursor = 0)
        val result = RichTextEngine.toggleFormat(state, SpanType.H1)
        assertThat(result.pendingBlockType).isEqualTo(SpanType.H1)
    }

    @Test
    fun `toggleFormat H1 on empty doc toggles off when already pending`() {
        val state = stateOf("", cursor = 0, pendingBlockType = SpanType.H1)
        val result = RichTextEngine.toggleFormat(state, SpanType.H1)
        assertThat(result.pendingBlockType).isNull()
    }

    @Test
    fun `toggleFormat H2 replaces pending H1`() {
        val state = stateOf("", cursor = 0, pendingBlockType = SpanType.H1)
        val result = RichTextEngine.toggleFormat(state, SpanType.H2)
        assertThat(result.pendingBlockType).isEqualTo(SpanType.H2)
    }

    // ── toggleFormat: bullet ────────────────────────────────────────────

    @Test
    fun `toggleFormat BULLET on non-empty line adds bullet prefix and span`() {
        val state = stateOf("Hello", cursor = 3)
        val result = RichTextEngine.toggleFormat(state, SpanType.BULLET)
        assertThat(result.document.rawText).startsWith("• ")
        assertThat(result.pendingBullet).isTrue()
    }

    @Test
    fun `toggleFormat BULLET on empty doc sets pending bullet`() {
        val state = stateOf("", cursor = 0)
        val result = RichTextEngine.toggleFormat(state, SpanType.BULLET)
        assertThat(result.pendingBullet).isTrue()
    }

    @Test
    fun `toggleFormat BULLET on empty doc toggles off when already pending`() {
        val state = stateOf("", cursor = 0, pendingBullet = true)
        val result = RichTextEngine.toggleFormat(state, SpanType.BULLET)
        assertThat(result.pendingBullet).isFalse()
    }

    @Test
    fun `toggleFormat BULLET removes bullet prefix when toggling off`() {
        val state = stateOf(
            "• Hello",
            spans = listOf(RichSpan(SpanType.BULLET, 0, 7)),
            cursor = 5,
            pendingBullet = true
        )
        val result = RichTextEngine.toggleFormat(state, SpanType.BULLET)
        assertThat(result.document.rawText).isEqualTo("Hello")
        assertThat(result.pendingBullet).isFalse()
    }

    // ── toggleFormat: bullet + block type coexistence ────────────────────

    @Test
    fun `bullet and H1 can coexist on same line`() {
        val state = stateOf("Hello", cursor = 3)
        var result = RichTextEngine.toggleFormat(state, SpanType.H1)
        result = RichTextEngine.toggleFormat(result, SpanType.BULLET)
        assertThat(result.document.rawText).startsWith("• ")
        assertThat(result.pendingBullet).isTrue()
        // H1 should still be active (block types are independent of bullet)
        assertThat(result.document.spans.any { it.type == SpanType.H1 }).isTrue()
    }

    // ── activeStyles ────────────────────────────────────────────────────

    @Test
    fun `activeStyles combines pending styles, block type, and bullet`() {
        val state = RichTextState(
            pendingStyles = setOf(SpanType.BOLD, SpanType.ITALIC),
            pendingBlockType = SpanType.H1,
            pendingBullet = true
        )
        assertThat(state.activeStyles).containsExactly(
            SpanType.BOLD, SpanType.ITALIC, SpanType.H1, SpanType.BULLET
        )
    }

    @Test
    fun `activeStyles with no pending state is empty`() {
        val state = RichTextState.EMPTY
        assertThat(state.activeStyles).isEmpty()
    }

    // ── Multi-character paste ───────────────────────────────────────────

    @Test
    fun `pasting text with pending bold applies bold to pasted range`() {
        val state = stateOf("AB", pendingStyles = setOf(SpanType.BOLD), cursor = 2)
        val result = RichTextEngine.onValueChanged(state, tfv("ABXYZ", 5))
        assertThat(result.document.isActiveAt(SpanType.BOLD, 2, 5)).isTrue()
    }

    @Test
    fun `pasting multiline text with pending block type applies to all new lines`() {
        val state = stateOf("", pendingBlockType = SpanType.H1, cursor = 0)
        val result = RichTextEngine.onValueChanged(state, tfv("Line1\nLine2", 11))
        assertThat(result.document.isActiveAt(SpanType.H1, 0, 6)).isTrue()
    }
}
