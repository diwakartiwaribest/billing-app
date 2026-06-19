package com.shop.billing.data.sync

data class LogEntry(
    val timestamp: String,
    val message: String,
    val type: LogType = LogType.INFO,
    val timestampMillis: Long = System.currentTimeMillis()
)

enum class LogType { INFO, SUCCESS, ERROR }
