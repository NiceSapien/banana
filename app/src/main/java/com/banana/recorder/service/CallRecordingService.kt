package com.banana.recorder.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.banana.recorder.data.RecordingsRepository
import com.banana.recorder.ui.theme.BananaRecorderTheme
import kotlinx.coroutines.*
import java.io.File

class CallRecordingService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    private var mediaRecorder: MediaRecorder? = null
    private var recordingFilePath: String? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var overlayView: ComposeView? = null
    private var windowManager: WindowManager? = null
    
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    
    private var isPaused = mutableStateOf(false)
    private var recordingDuration = mutableStateOf(0)

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> {
                val phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER) ?: "Unknown"
                val isIncoming = intent.getBooleanExtra(EXTRA_IS_INCOMING, true)
                val contactName = intent.getStringExtra(EXTRA_CONTACT_NAME)
                startRecording(phoneNumber, isIncoming, contactName)
            }
            ACTION_STOP_RECORDING -> {
                stopRecording()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startRecording(phoneNumber: String, isIncoming: Boolean, contactName: String?) {
        try {
            // Show toast for debugging
            Toast.makeText(this, "banana", Toast.LENGTH_SHORT).show()
            
            // Show visible overlay with controls
            showOverlay(phoneNumber, isIncoming, contactName)
            
            val repository = RecordingsRepository(this)
            recordingFilePath = repository.getRecordingFilePath(phoneNumber, isIncoming, contactName)

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(recordingFilePath)
                
                try {
                    prepare()
                    start()
                    
                    // Start duration counter
                    startDurationCounter()
                    
                    val notification = createNotification("Recording call from $phoneNumber")
                    startForeground(NOTIFICATION_ID, notification)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this@CallRecordingService, "Recording failed: ${e.message}", Toast.LENGTH_LONG).show()
                    // If recording fails, clean up
                    recordingFilePath?.let { File(it).delete() }
                    hideOverlay()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Recording error: ${e.message}", Toast.LENGTH_LONG).show()
            hideOverlay()
        }
    }
    
    private var durationJob: Job? = null
    
    private fun startDurationCounter() {
        durationJob?.cancel()
        recordingDuration.value = 0
        durationJob = serviceScope.launch {
            while (isActive && !isPaused.value) {
                delay(1000)
                recordingDuration.value++
            }
        }
    }
    
    private fun pauseRecording() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.pause()
                isPaused.value = true
                durationJob?.cancel()
                Toast.makeText(this, "Recording paused", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Pause failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun resumeRecording() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.resume()
                isPaused.value = false
                startDurationCounter()
                Toast.makeText(this, "Recording resumed", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Resume failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showOverlay(phoneNumber: String, isIncoming: Boolean, contactName: String?) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                return
            }
            
            lifecycleRegistry.currentState = Lifecycle.State.STARTED
            
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            overlayView = ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@CallRecordingService)
                setViewTreeSavedStateRegistryOwner(this@CallRecordingService)
                
                setContent {
                    BananaRecorderTheme {
                        RecordingOverlayUI(
                            phoneNumber = phoneNumber,
                            isIncoming = isIncoming,
                            contactName = contactName,
                            isPaused = isPaused.value,
                            duration = recordingDuration.value,
                            onPause = {
                                if (isPaused.value) {
                                    resumeRecording()
                                } else {
                                    pauseRecording()
                                }
                            },
                            onStop = {
                                stopRecording()
                                stopSelf()
                            }
                        )
                    }
                }
            }
            
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                x = 16
                y = 100
            }
            
            windowManager?.addView(overlayView, params)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Overlay error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun hideOverlay() {
        try {
            overlayView?.let {
                windowManager?.removeView(it)
            }
            overlayView = null
            windowManager = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopRecording() {
        try {
            durationJob?.cancel()
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            hideOverlay()
        } catch (e: Exception) {
            e.printStackTrace()
            // If stop fails, delete the possibly corrupted file
            recordingFilePath?.let { File(it).delete() }
            hideOverlay()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Call Recording",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when a call is being recorded"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Banana Recorder")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
        hideOverlay()
        serviceScope.cancel()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }
    
    @Composable
    private fun RecordingOverlayUI(
        phoneNumber: String,
        isIncoming: Boolean,
        contactName: String?,
        isPaused: Boolean,
        duration: Int,
        onPause: () -> Unit,
        onStop: () -> Unit
    ) {
        Surface(
            modifier = Modifier
                .width(280.dp)
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 8.dp,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Text(
                    text = "🍌 Recording",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Call info
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = contactName ?: phoneNumber,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isIncoming) "Incoming" else "Outgoing",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Duration
                Text(
                    text = formatDuration(duration),
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (isPaused) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                
                // Status
                if (isPaused) {
                    Text(
                        text = "PAUSED",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Pause/Resume button (only on Android N+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        FilledTonalIconButton(
                            onClick = onPause,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if (isPaused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = if (isPaused) Icons.Default.Pause else Icons.Default.Pause,
                                contentDescription = if (isPaused) "Resume" else "Pause",
                                tint = if (isPaused) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    
                    // Stop button
                    FilledIconButton(
                        onClick = onStop,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop",
                            tint = MaterialTheme.colorScheme.onError
                        )
                    }
                }
            }
        }
    }
    
    private fun formatDuration(seconds: Int): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", mins, secs)
    }

    companion object {
        private const val CHANNEL_ID = "call_recording_channel"
        private const val NOTIFICATION_ID = 1

        const val ACTION_START_RECORDING = "com.banana.recorder.START_RECORDING"
        const val ACTION_STOP_RECORDING = "com.banana.recorder.STOP_RECORDING"
        const val EXTRA_PHONE_NUMBER = "phone_number"
        const val EXTRA_IS_INCOMING = "is_incoming"
        const val EXTRA_CONTACT_NAME = "contact_name"

        fun startRecording(context: Context, phoneNumber: String, isIncoming: Boolean, contactName: String?) {
            val intent = Intent(context, CallRecordingService::class.java).apply {
                action = ACTION_START_RECORDING
                putExtra(EXTRA_PHONE_NUMBER, phoneNumber)
                putExtra(EXTRA_IS_INCOMING, isIncoming)
                putExtra(EXTRA_CONTACT_NAME, contactName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopRecording(context: Context) {
            val intent = Intent(context, CallRecordingService::class.java).apply {
                action = ACTION_STOP_RECORDING
            }
            context.startService(intent)
        }
    }
}
