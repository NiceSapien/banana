package com.banana.recorder.model

enum class RecordingMode {
    ALL_CALLS,
    SPECIFIC_CONTACTS,
    UNKNOWN_CALLERS,
    ALL_CONTACTS,
    UNKNOWN_AND_SPECIFIC
}

data class RecordingSettings(
    val mode: RecordingMode = RecordingMode.ALL_CALLS,
    val selectedContactIds: Set<String> = emptySet(),
    val scheduleEnabled: Boolean = false,
    val selectedDays: Set<Int> = (1..7).toSet(), // 1 = Monday, 7 = Sunday
    val startTimeHour: Int = 0,
    val startTimeMinute: Int = 0,
    val endTimeHour: Int = 23,
    val endTimeMinute: Int = 59,
    val autoDeleteEnabled: Boolean = false,
    val autoDeleteDays: Int = 30
)
