package com.shop.billing.data.local.entity

import androidx.room.ColumnInfo
import java.time.Instant

enum class SyncStatus {
    SYNCED,
    PENDING_CREATE,
    PENDING_UPDATE,
    PENDING_DELETE,
    FAILED
}

data class SyncMetadata(
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val deleted: Boolean = false,
    val version: Int = 1,
    val ownerId: String = "",
    val syncStatus: SyncStatus = SyncStatus.PENDING_CREATE,
    val lastSyncAttemptAt: Instant? = null,
    val syncError: String? = null
)
