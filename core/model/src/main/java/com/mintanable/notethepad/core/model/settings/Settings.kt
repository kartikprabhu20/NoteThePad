package com.mintanable.notethepad.core.model.settings

data class Settings(
    val notificationsEnabled: Boolean = true,
    val backupEnabled: Boolean = false,
    val supaSyncEnabled: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val googleAccount: String? = null,
    val backupSettings: BackupSettings = BackupSettings(),
    val aiModelName: String = "None",
    val aiAssistantEnabled: Boolean = false,
    val noteShape: NoteShape = NoteShape.DEFAULT,
    val onboardingCompleted: Boolean = true
)

data class BackupSettings(
    val backupFrequency: BackupFrequency = BackupFrequency.OFF,
    val backupTimeHour: Int = 2,
    val backupTimeMinutes: Int = 0,
    val backupMedia: Boolean = false
)

enum class ThemeMode { LIGHT, DARK, SYSTEM }

enum class BackupFrequency(val days: Long, val title: String) {
    OFF(0, "Off"),
    DAILY(1, "Daily"),
    WEEKLY(7, "Weekly"),
    MONTHLY(30, "Monthly")
}