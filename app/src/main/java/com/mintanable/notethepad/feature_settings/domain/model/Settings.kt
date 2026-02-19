package com.mintanable.notethepad.feature_settings.domain.model

data class Settings(
    val notificationsEnabled: Boolean = true,
    val backupEnabled: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isGridViewSelected: Boolean = false
)

enum class ThemeMode { LIGHT, DARK, SYSTEM }