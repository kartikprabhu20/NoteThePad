package com.mintanable.notethepad

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

data class NoteColorPair(val light: Color, val dark: Color)

data class NoteBackgroundImage(val lightRes: Int, val darkRes: Int)

object NoteColors {
    // 10 pastel + dark pairs
    val colorPairs = listOf(
        NoteColorPair(light = Color(0xfffffffe), dark = Color(0xff000001)),   // white
        NoteColorPair(light = Color(0xffffab91), dark = Color(0xff8d4e3a)),   // RedOrange
        NoteColorPair(light = Color(0xfff48fb1), dark = Color(0xff8c3a5e)),   // RedPink
        NoteColorPair(light = Color(0xff81deea), dark = Color(0xff2a7a8a)),   // BabyBlue
        NoteColorPair(light = Color(0xffcf94da), dark = Color(0xff6b3f7a)),   // Violet
        NoteColorPair(light = Color(0xffe7ed9b), dark = Color(0xff6e7a2e)),   // LightGreen
        NoteColorPair(light = Color(0xffffd59e), dark = Color(0xff8a6530)),   // PeachPuff
        NoteColorPair(light = Color(0xffa0d2f0), dark = Color(0xff2e6a8e)),   // SkyBlue
        NoteColorPair(light = Color(0xffc5b3e6), dark = Color(0xff4e3878)),   // Lavender
        NoteColorPair(light = Color(0xffa8e6cf), dark = Color(0xff2e7a56)),   // MintGreen
        NoteColorPair(light = Color(0xfffff9b0), dark = Color(0xff7a7530)),   // LemonYellow
    )

    // Light colors list for backward compat (random new note color, etc.)
    val colors: List<Color> = colorPairs.map { it.light }

    // 10 background image slots (placeholder - 0 means no resource yet)
    val backgroundImages = listOf(
        NoteBackgroundImage(lightRes = 0, darkRes = 0),
        NoteBackgroundImage(lightRes = 0, darkRes = 0),
        NoteBackgroundImage(lightRes = 0, darkRes = 0),
        NoteBackgroundImage(lightRes = 0, darkRes = 0),
        NoteBackgroundImage(lightRes = 0, darkRes = 0),
        NoteBackgroundImage(lightRes = 0, darkRes = 0),
        NoteBackgroundImage(lightRes = 0, darkRes = 0),
        NoteBackgroundImage(lightRes = 0, darkRes = 0),
        NoteBackgroundImage(lightRes = 0, darkRes = 0),
        NoteBackgroundImage(lightRes = 0, darkRes = 0),
    )

    private val lightToColorPairMap: Map<Int, NoteColorPair> by lazy {
        colorPairs.associateBy { it.light.toArgb() }
    }

    fun resolveDisplayColor(lightColorArgb: Int, isDarkTheme: Boolean): Color {
        if (lightColorArgb == -1) return Color.Transparent
        val pair = lightToColorPairMap[lightColorArgb]
        return if (pair != null && isDarkTheme) pair.dark else Color(lightColorArgb)
    }

    fun resolveBackgroundImage(index: Int, isDarkTheme: Boolean): Int {
        if (index < 0 || index >= backgroundImages.size) return 0
        val pair = backgroundImages[index]
        return if (isDarkTheme) pair.darkRes else pair.lightRes
    }
}
