package com.banana.recorder

import android.app.Application
import androidx.work.*
import com.banana.recorder.data.RecordingsRepository
import com.banana.recorder.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class BananaApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        
        // Schedule daily auto-delete work
        scheduleAutoDeleteWork()
    }

    private fun scheduleAutoDeleteWork() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val autoDeleteRequest = PeriodicWorkRequestBuilder<AutoDeleteWorker>(
            1, TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "auto_delete_recordings",
            ExistingPeriodicWorkPolicy.KEEP,
            autoDeleteRequest
        )
    }
}

class AutoDeleteWorker(
    context: android.content.Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settingsRepository = SettingsRepository(applicationContext)
        val recordingsRepository = RecordingsRepository(applicationContext)

        val settings = settingsRepository.settingsFlow.first()
        
        if (settings.autoDeleteEnabled) {
            recordingsRepository.deleteOldRecordings(settings.autoDeleteDays)
        }

        return Result.success()
    }
}
