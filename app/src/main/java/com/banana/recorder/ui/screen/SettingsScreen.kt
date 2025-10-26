package com.banana.recorder.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.banana.recorder.model.RecordingMode
import com.banana.recorder.model.RecordingSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: RecordingSettings,
    onSettingsChange: (RecordingSettings) -> Unit,
    onBackClick: () -> Unit,
    onSelectContactsClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Recording Mode Section
            RecordingModeSection(
                selectedMode = settings.mode,
                selectedContactsCount = settings.selectedContactIds.size,
                onModeChange = { mode ->
                    onSettingsChange(settings.copy(mode = mode))
                },
                onSelectContactsClick = onSelectContactsClick
            )

            Divider()

            // Schedule Section
            ScheduleSection(
                scheduleEnabled = settings.scheduleEnabled,
                selectedDays = settings.selectedDays,
                startTimeHour = settings.startTimeHour,
                startTimeMinute = settings.startTimeMinute,
                endTimeHour = settings.endTimeHour,
                endTimeMinute = settings.endTimeMinute,
                onScheduleEnabledChange = { enabled ->
                    onSettingsChange(settings.copy(scheduleEnabled = enabled))
                },
                onDaysChange = { days ->
                    onSettingsChange(settings.copy(selectedDays = days))
                },
                onStartTimeChange = { hour, minute ->
                    onSettingsChange(settings.copy(startTimeHour = hour, startTimeMinute = minute))
                },
                onEndTimeChange = { hour, minute ->
                    onSettingsChange(settings.copy(endTimeHour = hour, endTimeMinute = minute))
                }
            )

            Divider()

            // Auto-Delete Section
            AutoDeleteSection(
                autoDeleteEnabled = settings.autoDeleteEnabled,
                autoDeleteDays = settings.autoDeleteDays,
                onAutoDeleteEnabledChange = { enabled ->
                    onSettingsChange(settings.copy(autoDeleteEnabled = enabled))
                },
                onAutoDeleteDaysChange = { days ->
                    onSettingsChange(settings.copy(autoDeleteDays = days))
                }
            )
        }
    }
}

@Composable
fun RecordingModeSection(
    selectedMode: RecordingMode,
    selectedContactsCount: Int,
    onModeChange: (RecordingMode) -> Unit,
    onSelectContactsClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Recording Mode",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        
        RecordingMode.values().forEach { mode ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedMode == mode,
                    onClick = { onModeChange(mode) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (mode) {
                        RecordingMode.ALL_CALLS -> "Record All Calls"
                        RecordingMode.SPECIFIC_CONTACTS -> "Record from Specific Contacts"
                        RecordingMode.UNKNOWN_CALLERS -> "Record from Unknown Callers"
                        RecordingMode.ALL_CONTACTS -> "Record from All Contacts"
                        RecordingMode.UNKNOWN_AND_SPECIFIC -> "Record from Unknown & Specific Contacts"
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        
        // Show contact selection button for relevant modes
        if (selectedMode == RecordingMode.SPECIFIC_CONTACTS || selectedMode == RecordingMode.UNKNOWN_AND_SPECIFIC) {
            Spacer(modifier = Modifier.height(8.dp))
            FilledTonalButton(
                onClick = onSelectContactsClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (selectedContactsCount > 0) {
                        "Selected Contacts ($selectedContactsCount)"
                    } else {
                        "Select Contacts"
                    }
                )
            }
        }
    }
}

@Composable
fun ScheduleSection(
    scheduleEnabled: Boolean,
    selectedDays: Set<Int>,
    startTimeHour: Int,
    startTimeMinute: Int,
    endTimeHour: Int,
    endTimeMinute: Int,
    onScheduleEnabledChange: (Boolean) -> Unit,
    onDaysChange: (Set<Int>) -> Unit,
    onStartTimeChange: (Int, Int) -> Unit,
    onEndTimeChange: (Int, Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recording Schedule",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Switch(
                checked = scheduleEnabled,
                onCheckedChange = onScheduleEnabledChange
            )
        }

        if (scheduleEnabled) {
            // Days selection
            Text(
                text = "Select Days",
                style = MaterialTheme.typography.titleMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val dayNames = listOf("M", "T", "W", "T", "F", "S", "S")
                dayNames.forEachIndexed { index, day ->
                    val dayNumber = index + 1
                    FilterChip(
                        selected = dayNumber in selectedDays,
                        onClick = {
                            val newDays = if (dayNumber in selectedDays) {
                                selectedDays - dayNumber
                            } else {
                                selectedDays + dayNumber
                            }
                            onDaysChange(newDays)
                        },
                        label = { Text(day) }
                    )
                }
            }

            // Time range
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Start Time",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = String.format("%02d:%02d", startTimeHour, startTimeMinute),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "End Time",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = String.format("%02d:%02d", endTimeHour, endTimeMinute),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
fun AutoDeleteSection(
    autoDeleteEnabled: Boolean,
    autoDeleteDays: Int,
    onAutoDeleteEnabledChange: (Boolean) -> Unit,
    onAutoDeleteDaysChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Auto-Delete Old Recordings",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Switch(
                checked = autoDeleteEnabled,
                onCheckedChange = onAutoDeleteEnabledChange
            )
        }

        if (autoDeleteEnabled) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Delete After (Days)",
                    style = MaterialTheme.typography.bodyLarge
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { if (autoDeleteDays > 1) onAutoDeleteDaysChange(autoDeleteDays - 1) }
                    ) {
                        Text("-")
                    }
                    Text(
                        text = autoDeleteDays.toString(),
                        style = MaterialTheme.typography.titleMedium
                    )
                    FilledTonalButton(
                        onClick = { onAutoDeleteDaysChange(autoDeleteDays + 1) }
                    ) {
                        Text("+")
                    }
                }
            }
        }
    }
}
