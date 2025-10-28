package com.banana.recorder.model

data class Recording(
    val id: String,
    val phoneNumber: String,
    val contactName: String?,
    val timestamp: Long,
    val duration: Long,
    val filePath: String,
    val isIncoming: Boolean
)
