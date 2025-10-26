package com.banana.recorder.data

import android.content.Context
import com.banana.recorder.model.Recording
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class RecordingsRepository(private val context: Context) {
    
    private val recordingsDir: File
        get() = File(context.getExternalFilesDir(null), "recordings").also { it.mkdirs() }

    suspend fun getAllRecordings(): List<Recording> = withContext(Dispatchers.IO) {
        val files = recordingsDir.listFiles()?.filter { it.extension == "mp3" || it.extension == "m4a" } ?: emptyList()
        files.mapNotNull { file ->
            parseRecordingFileName(file)
        }.sortedByDescending { it.timestamp }
    }

    private fun parseRecordingFileName(file: File): Recording? {
        // Expected format: {timestamp}_{number}_{incoming/outgoing}_{name}.mp3
        val parts = file.nameWithoutExtension.split("_")
        if (parts.size < 3) return null
        
        return try {
            Recording(
                id = file.nameWithoutExtension,
                phoneNumber = parts.getOrNull(1)?.replace("-", "") ?: "Unknown",
                contactName = if (parts.size > 3) parts.drop(3).joinToString("_") else null,
                timestamp = parts[0].toLongOrNull() ?: file.lastModified(),
                duration = 0L, // Calculate later if needed
                filePath = file.absolutePath,
                isIncoming = parts.getOrNull(2) == "incoming"
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteRecording(recording: Recording): Boolean = withContext(Dispatchers.IO) {
        File(recording.filePath).delete()
    }

    suspend fun deleteOldRecordings(daysOld: Int): Int = withContext(Dispatchers.IO) {
        val cutoffTime = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)
        val files = recordingsDir.listFiles()?.filter { 
            (it.extension == "mp3" || it.extension == "m4a") && it.lastModified() < cutoffTime 
        } ?: emptyList()
        
        files.forEach { it.delete() }
        files.size
    }

    fun getRecordingFilePath(phoneNumber: String, isIncoming: Boolean, contactName: String?): String {
        val timestamp = System.currentTimeMillis()
        val type = if (isIncoming) "incoming" else "outgoing"
        val sanitizedNumber = phoneNumber.replace(Regex("[^0-9]"), "")
        val sanitizedName = contactName?.replace(Regex("[^a-zA-Z0-9]"), "") ?: "unknown"
        val fileName = "${timestamp}_${sanitizedNumber}_${type}_${sanitizedName}.m4a"
        return File(recordingsDir, fileName).absolutePath
    }
}
