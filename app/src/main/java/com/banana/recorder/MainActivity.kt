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
        permissionsGranted.value = allGranted
        if (allGranted) {
            loadRecordings()
        }
        // Don't open settings automatically - user can try again from onboarding screen
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
        // First request regular permissions
        permissionLauncher.launch(PermissionUtils.getRequiredPermissions())
        
        // Then request overlay permission if on Android M+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Recheck permissions when returning from overlay permission screen
        permissionsGranted.value = PermissionUtils.hasAllPermissions(this)
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
