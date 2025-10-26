package com.banana.recorder

import android.Manifest
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.banana.recorder.data.RecordingsRepository
import com.banana.recorder.data.SettingsRepository
import com.banana.recorder.model.Recording
import com.banana.recorder.model.RecordingSettings
import com.banana.recorder.ui.screen.ContactSelectionScreen
import com.banana.recorder.ui.screen.OnboardingScreen
import com.banana.recorder.ui.screen.RecordingsScreen
import com.banana.recorder.ui.screen.SettingsScreen
import com.banana.recorder.ui.theme.BananaTheme
import com.banana.recorder.util.PermissionUtils
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {

    private val recordingsRepository by lazy { RecordingsRepository(this) }
    private val settingsRepository by lazy { SettingsRepository(this) }
    private var mediaPlayer: MediaPlayer? = null
    private var permissionsGranted = mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            permissionsGranted.value = true
            loadRecordings()
        } else {
            // Some permissions denied - guide user to settings
            if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
                // User denied but didn't check "Don't ask again"
                // Can show explanation and request again
            } else {
                // User checked "Don't ask again" - guide to settings
                openAppSettings()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        permissionsGranted.value = PermissionUtils.hasAllPermissions(this)

        setContent {
            BananaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BananaApp()
                }
            }
        }
    }

    @Composable
    fun BananaApp() {
        val navController = rememberNavController()
        var recordings by remember { mutableStateOf<List<Recording>>(emptyList()) }
        val settings by settingsRepository.settingsFlow.collectAsState(initial = RecordingSettings())
        val hasPermissions by remember { permissionsGranted }

        LaunchedEffect(hasPermissions) {
            if (hasPermissions) {
                recordings = recordingsRepository.getAllRecordings()
            }
        }

        if (!hasPermissions) {
            OnboardingScreen(
                onPermissionsRequest = {
                    requestPermissions()
                }
            )
        } else {
            NavHost(navController = navController, startDestination = "recordings") {
                composable("recordings") {
                    RecordingsScreen(
                        recordings = recordings,
                        onDeleteRecording = { recording ->
                            lifecycleScope.launch {
                                recordingsRepository.deleteRecording(recording)
                                recordings = recordingsRepository.getAllRecordings()
                            }
                        },
                        onPlayRecording = { recording ->
                            playRecording(recording)
                        },
                        onSettingsClick = {
                            navController.navigate("settings")
                        }
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        settings = settings,
                        onSettingsChange = { newSettings ->
                            lifecycleScope.launch {
                                settingsRepository.updateSettings(newSettings)
                            }
                        },
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onSelectContactsClick = {
                            navController.navigate("contacts")
                        }
                    )
                }
                composable("contacts") {
                    ContactSelectionScreen(
                        selectedContactIds = settings.selectedContactIds,
                        onContactsSelected = { selectedIds ->
                            lifecycleScope.launch {
                                settingsRepository.updateSettings(
                                    settings.copy(selectedContactIds = selectedIds)
                                )
                            }
                        },
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                }
            }

            // Auto-delete old recordings
            LaunchedEffect(settings.autoDeleteEnabled, settings.autoDeleteDays) {
                if (settings.autoDeleteEnabled) {
                    recordingsRepository.deleteOldRecordings(settings.autoDeleteDays)
                    recordings = recordingsRepository.getAllRecordings()
                }
            }
        }
    }

    private fun requestPermissions() {
        permissionLauncher.launch(PermissionUtils.getRequiredPermissions())
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    private fun loadRecordings() {
        lifecycleScope.launch {
            val allRecordings = recordingsRepository.getAllRecordings()
            // Update state if needed
        }
    }

    private fun playRecording(recording: Recording) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(recording.filePath)
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }
}
