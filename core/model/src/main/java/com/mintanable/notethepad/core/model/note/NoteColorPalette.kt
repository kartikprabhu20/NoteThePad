package com.mintanable.notethepad.core.model.note

object NoteColorPalette {

    val COLOR_NAME_TO_ARGB: Map<String, Int> = linkedMapOf(
        "white" to 0xfffffffe.toInt(),
        "redOrange" to 0xffffab91.toInt(),
        "redPink" to 0xfff48fb1.toInt(),
        "babyBlue" to 0xff81deea.toInt(),
        "violet" to 0xffcf94da.toInt(),
        "lightGreen" to 0xffe7ed9b.toInt(),
        "peachPuff" to 0xffffd59e.toInt(),
        "skyBlue" to 0xffa0d2f0.toInt(),
        "lavender" to 0xffc5b3e6.toInt(),
        "mintGreen" to 0xffa8e6cf.toInt(),
        "lemonYellow" to 0xfffff9b0.toInt(),
    )

    private val ALIASES: Map<String, String> = mapOf(
        "yellow" to "lemonYellow",
        "lemon" to "lemonYellow",
        "orange" to "redOrange",
        "red" to "redOrange",
        "pink" to "redPink",
        "blue" to "babyBlue",
        "sky" to "skyBlue",
        "purple" to "violet",
        "green" to "lightGreen",
        "mint" to "mintGreen",
        "peach" to "peachPuff",
    )

    fun findArgb(colorName: String): Int? {
        val normalized = colorName.trim().lowercase()
        val canonical = COLOR_NAME_TO_ARGB.keys.firstOrNull { it.lowercase() == normalized }
        if (canonical != null) return COLOR_NAME_TO_ARGB[canonical]
        val aliased = ALIASES[normalized] ?: return null
        return COLOR_NAME_TO_ARGB[aliased]
    }

    val validNames: List<String> get() = COLOR_NAME_TO_ARGB.keys.toList()
}
