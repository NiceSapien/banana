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
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.banana.recorder.data.RecordingsRepository
import kotlinx.coroutines.*
import java.io.File

class CallRecordingService : Service() {

    private var mediaRecorder: MediaRecorder? = null
    private var recordingFilePath: String? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var overlayView: FrameLayout? = null
    private var windowManager: WindowManager? = null

    override fun onCreate() {
        super.onCreate()
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
            
            // Show invisible overlay to keep app "in use" for microphone permission
            showOverlay()
            
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
    
    private fun showOverlay() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                return
            }
            
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            overlayView = FrameLayout(this).apply {
                // Invisible 1x1 pixel overlay
                alpha = 0.01f
            }
            
            val params = WindowManager.LayoutParams(
                1, // width
                1, // height
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
            }
            
            windowManager?.addView(overlayView, params)
        } catch (e: Exception) {
            e.printStackTrace()
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
