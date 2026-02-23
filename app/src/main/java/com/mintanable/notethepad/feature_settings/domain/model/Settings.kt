package com.mintanable.notethepad.feature_settings.domain.model

data class Settings(
    val notificationsEnabled: Boolean = true,
    val backupEnabled: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isGridViewSelected: Boolean = false,
    val googleAccount: String? = null,
    val backupSettings: BackupSettings = BackupSettings()
)

data class BackupSettings(
    val backupFrequency: BackupFrequency = BackupFrequency.OFF,
    val backupTimeHour: Int = 2,
    val backupTimeMinutes: Int = 0
)
enum class ThemeMode { LIGHT, DARK, SYSTEM }

enum class BackupFrequency(val days: Long, val title: String) {
    OFF(0, "Off"),
    DAILY(1, "Daily"),
    WEEKLY(7, "Weekly"),
    MONTHLY(30, "Monthly")
}