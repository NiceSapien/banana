package com.banana.recorder.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.banana.recorder.model.RecordingMode
import com.banana.recorder.model.RecordingSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    
    private object PreferencesKeys {
        val RECORDING_MODE = stringPreferencesKey("recording_mode")
        val SELECTED_CONTACTS = stringSetPreferencesKey("selected_contacts")
        val SCHEDULE_ENABLED = booleanPreferencesKey("schedule_enabled")
        val SELECTED_DAYS = stringPreferencesKey("selected_days")
        val START_TIME_HOUR = intPreferencesKey("start_time_hour")
        val START_TIME_MINUTE = intPreferencesKey("start_time_minute")
        val END_TIME_HOUR = intPreferencesKey("end_time_hour")
        val END_TIME_MINUTE = intPreferencesKey("end_time_minute")
        val AUTO_DELETE_ENABLED = booleanPreferencesKey("auto_delete_enabled")
        val AUTO_DELETE_DAYS = intPreferencesKey("auto_delete_days")
    }

    val settingsFlow: Flow<RecordingSettings> = context.dataStore.data.map { preferences ->
        RecordingSettings(
            mode = RecordingMode.valueOf(
                preferences[PreferencesKeys.RECORDING_MODE] ?: RecordingMode.ALL_CALLS.name
            ),
            selectedContactIds = preferences[PreferencesKeys.SELECTED_CONTACTS] ?: emptySet(),
            scheduleEnabled = preferences[PreferencesKeys.SCHEDULE_ENABLED] ?: false,
            selectedDays = preferences[PreferencesKeys.SELECTED_DAYS]?.split(",")
                ?.mapNotNull { it.toIntOrNull() }?.toSet() ?: (1..7).toSet(),
            startTimeHour = preferences[PreferencesKeys.START_TIME_HOUR] ?: 0,
            startTimeMinute = preferences[PreferencesKeys.START_TIME_MINUTE] ?: 0,
            endTimeHour = preferences[PreferencesKeys.END_TIME_HOUR] ?: 23,
            endTimeMinute = preferences[PreferencesKeys.END_TIME_MINUTE] ?: 59,
            autoDeleteEnabled = preferences[PreferencesKeys.AUTO_DELETE_ENABLED] ?: false,
            autoDeleteDays = preferences[PreferencesKeys.AUTO_DELETE_DAYS] ?: 30
        )
    }

    suspend fun updateSettings(settings: RecordingSettings) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.RECORDING_MODE] = settings.mode.name
            preferences[PreferencesKeys.SELECTED_CONTACTS] = settings.selectedContactIds
            preferences[PreferencesKeys.SCHEDULE_ENABLED] = settings.scheduleEnabled
            preferences[PreferencesKeys.SELECTED_DAYS] = settings.selectedDays.joinToString(",")
            preferences[PreferencesKeys.START_TIME_HOUR] = settings.startTimeHour
            preferences[PreferencesKeys.START_TIME_MINUTE] = settings.startTimeMinute
            preferences[PreferencesKeys.END_TIME_HOUR] = settings.endTimeHour
            preferences[PreferencesKeys.END_TIME_MINUTE] = settings.endTimeMinute
            preferences[PreferencesKeys.AUTO_DELETE_ENABLED] = settings.autoDeleteEnabled
            preferences[PreferencesKeys.AUTO_DELETE_DAYS] = settings.autoDeleteDays
        }
    }
}
