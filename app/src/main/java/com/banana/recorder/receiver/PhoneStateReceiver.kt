package com.banana.recorder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.banana.recorder.data.SettingsRepository
import com.banana.recorder.model.RecordingMode
import com.banana.recorder.service.CallRecordingService
import com.banana.recorder.util.ContactUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class PhoneStateReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            val phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

            when (state) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    // Incoming call
                    phoneNumber?.let {
                        scope.launch {
                            if (shouldRecord(context, it, true)) {
                                // Store for when call is answered
                                lastIncomingNumber = it
                            }
                        }
                    }
                }
                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    // Call answered
                    val number = phoneNumber ?: lastIncomingNumber
                    number?.let {
                        scope.launch {
                            if (shouldRecord(context, it, true)) {
                                val contactName = ContactUtils.getContactName(context, it)
                                CallRecordingService.startRecording(context, it, true, contactName)
                            }
                        }
                    }
                }
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    // Call ended
                    CallRecordingService.stopRecording(context)
                    lastIncomingNumber = null
                }
            }
        } else if (intent.action == Intent.ACTION_NEW_OUTGOING_CALL) {
            val phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
            phoneNumber?.let {
                scope.launch {
                    if (shouldRecord(context, it, false)) {
                        val contactName = ContactUtils.getContactName(context, it)
                        CallRecordingService.startRecording(context, it, false, contactName)
                    }
                }
            }
        }
    }

    private suspend fun shouldRecord(context: Context, phoneNumber: String, isIncoming: Boolean): Boolean {
        val repository = SettingsRepository(context)
        val settings = repository.settingsFlow.first()

        // Check schedule
        if (settings.scheduleEnabled) {
            val calendar = Calendar.getInstance()
            val currentDay = if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) 7 else calendar.get(Calendar.DAY_OF_WEEK) - 1
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)

            if (currentDay !in settings.selectedDays) {
                return false
            }

            val currentTime = currentHour * 60 + currentMinute
            val startTime = settings.startTimeHour * 60 + settings.startTimeMinute
            val endTime = settings.endTimeHour * 60 + settings.endTimeMinute

            if (currentTime !in startTime..endTime) {
                return false
            }
        }

        // Check recording mode
        val isContact = ContactUtils.isContactSaved(context, phoneNumber)

        return when (settings.mode) {
            RecordingMode.ALL_CALLS -> true
            RecordingMode.SPECIFIC_CONTACTS -> {
                // For simplicity, check if contact is saved (would need contact ID comparison in production)
                isContact
            }
            RecordingMode.UNKNOWN_CALLERS -> !isContact
            RecordingMode.ALL_CONTACTS -> isContact
            RecordingMode.UNKNOWN_AND_SPECIFIC -> {
                // Unknown or in specific list
                !isContact || isContact
            }
        }
    }

    companion object {
        private var lastIncomingNumber: String? = null
    }
}
