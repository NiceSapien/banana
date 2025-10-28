package com.banana.recorder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.widget.Toast
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
        Toast.makeText(context, "Call detected!", Toast.LENGTH_SHORT).show()
        
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            val phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

            when (state) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    // Incoming call ringing
                    Toast.makeText(context, "Incoming call ringing", Toast.LENGTH_SHORT).show()
                    phoneNumber?.let {
                        // Store incoming number for when call is answered
                        lastIncomingNumber = it
                        isIncomingCall = true
                    }
                }
                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    // Call answered/started (works for both incoming and outgoing)
                    Toast.makeText(context, "Call answered/started", Toast.LENGTH_SHORT).show()
                    
                    // Determine which number to use and if it's incoming
                    val numberToRecord: String?
                    val isIncoming: Boolean
                    
                    if (lastIncomingNumber != null) {
                        // This is an incoming call being answered
                        numberToRecord = lastIncomingNumber
                        isIncoming = true
                    } else if (lastOutgoingNumber != null) {
                        // This is an outgoing call being answered
                        numberToRecord = lastOutgoingNumber
                        isIncoming = false
                    } else {
                        // Fallback - shouldn't happen
                        numberToRecord = phoneNumber
                        isIncoming = isIncomingCall
                    }
                    
                    numberToRecord?.let { number ->
                        scope.launch {
                            if (shouldRecord(context, number, isIncoming)) {
                                val contactName = ContactUtils.getContactName(context, number)
                                CallRecordingService.startRecording(context, number, isIncoming, contactName)
                            }
                        }
                    }
                }
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    // Call ended
                    Toast.makeText(context, "Call ended", Toast.LENGTH_SHORT).show()
                    CallRecordingService.stopRecording(context)
                    // Reset all state
                    lastIncomingNumber = null
                    lastOutgoingNumber = null
                    isIncomingCall = true
                }
            }
        } else if (intent.action == Intent.ACTION_NEW_OUTGOING_CALL) {
            Toast.makeText(context, "Outgoing call dialing", Toast.LENGTH_SHORT).show()
            val phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
            phoneNumber?.let {
                // Store outgoing number for when call is answered (OFFHOOK state)
                lastOutgoingNumber = it
                isIncomingCall = false
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
        private var lastOutgoingNumber: String? = null
        private var isIncomingCall: Boolean = true
    }
}
