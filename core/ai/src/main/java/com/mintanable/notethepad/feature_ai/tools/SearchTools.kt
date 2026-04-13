package com.mintanable.notethepad.feature_ai.tools

import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolParam
import com.google.ai.edge.litertlm.ToolSet
import com.mintanable.notethepad.core.model.note.NoteColorPalette
import com.mintanable.notethepad.core.model.note.NoteOrder
import com.mintanable.notethepad.core.model.note.OrderType
import com.mintanable.notethepad.core.richtext.plaintext.extractPlaintext
import com.mintanable.notethepad.database.db.entity.NoteWithTags
import com.mintanable.notethepad.database.db.repository.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchTools @Inject constructor(
    private val noteRepository: NoteRepository,
) : ToolSet {

    @Tool(description = "Finds notes whose title or body text contains the given substring (case-insensitive). Searches the plaintext of rich content, not the raw JSON. Returns JSON array.")
    fun searchNotesByText(
        @ToolParam(description = "Substring to match") query: String,
        @ToolParam(description = "Max results (1-50)") limit: Int,
    ): String = runBlocking(Dispatchers.IO) {
        if (query.isBlank()) return@runBlocking "[]"
        val notes = noteRepository.getNotes(NoteOrder.Date(OrderType.Descending)).first()
        val q = query.trim().lowercase()
        val hits = notes.filter {
            val titleMatch = it.noteEntity.title.contains(q, ignoreCase = true)
            val bodyMatch = extractPlaintext(it.noteEntity.content).contains(q, ignoreCase = true)
            titleMatch || bodyMatch
        }.take(limit.coerceIn(1, 50))
        notesToJson(hits)
    }

    @Tool(description = "Finds notes tagged with the given tag name (case-insensitive). Returns JSON array.")
    fun searchNotesByTag(
        @ToolParam(description = "Tag name") tagName: String,
        @ToolParam(description = "Max results (1-50)") limit: Int,
    ): String = runBlocking(Dispatchers.IO) {
        if (tagName.isBlank()) return@runBlocking "[]"
        val notes = noteRepository.getNotes(NoteOrder.Date(OrderType.Descending)).first()
        val hits = notes.filter { nwt ->
            nwt.tagEntities.any { it.tagName.equals(tagName, ignoreCase = true) }
        }.take(limit.coerceIn(1, 50))
        notesToJson(hits)
    }

    @Tool(description = "Returns notes with future reminders in the [fromMs, toMs] window. Pass 0 for open bounds. Returns JSON array.")
    fun listNotesWithReminders(
        @ToolParam(description = "Start of window in UTC ms; 0 for no lower bound") fromMs: Double,
        @ToolParam(description = "End of window in UTC ms; 0 for no upper bound") toMs: Double,
        @ToolParam(description = "Max results (1-50)") limit: Int,
    ): String = runBlocking(Dispatchers.IO) {
        val all = noteRepository.getNotesWithFutureReminders(System.currentTimeMillis())
        val from = fromMs.toLong()
        val to = toMs.toLong()
        val hits = all
            .filter { nwt ->
                val r = nwt.noteEntity.reminderTime
                (from == 0L || r >= from) && (to == 0L || r <= to)
            }
            .sortedBy { it.noteEntity.reminderTime }
            .take(limit.coerceIn(1, 50))
        notesToJson(hits)
    }

    @Tool(description = "Returns notes matching a named color (e.g. 'yellow', 'redOrange', 'babyBlue'). Valid names: white, redOrange, redPink, babyBlue, violet, lightGreen, peachPuff, skyBlue, lavender, mintGreen, lemonYellow. Common aliases like 'yellow', 'blue', 'green' are also accepted. Returns JSON array.")
    fun listNotesByColor(
        @ToolParam(description = "Color name, e.g. 'yellow' or 'lemonYellow'") colorName: String,
        @ToolParam(description = "Max results (1-50)") limit: Int,
    ): String = runBlocking(Dispatchers.IO) {
        val argb = NoteColorPalette.findArgb(colorName)
            ?: return@runBlocking JSONObject().apply {
                put("error", "unknown_color")
                put("valid", JSONArray(NoteColorPalette.validNames))
            }.toString()
        val notes = noteRepository.getNotes(NoteOrder.Date(OrderType.Descending)).first()
        val hits = notes.filter { it.noteEntity.color == argb }
            .take(limit.coerceIn(1, 50))
        notesToJson(hits)
    }

    private fun notesToJson(notes: List<NoteWithTags>): String {
        val arr = JSONArray()
        notes.forEach { nwt ->
            val n = nwt.noteEntity
            arr.put(JSONObject().apply {
                put("id", n.id)
                put("title", n.title)
                put("timestamp", n.timestamp)
                put("reminderMs", n.reminderTime)
                put("tags", JSONArray(nwt.tagEntities.map { it.tagName }))
            })
        }
        return arr.toString()
    }
}
