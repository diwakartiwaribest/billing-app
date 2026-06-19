package com.shop.billing.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val mobile: String,
    val name: String,
    val totalBills: Int = 0,
    val totalSpent: Double = 0.0,
    val pendingAmount: Double = 0.0,
    val creditAmount: Double = 0.0,
    val shopCode: String = "",
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val deleted: Boolean = false,
    val version: Int = 1,
    val ownerId: String = "",
    val syncStatus: SyncStatus = SyncStatus.PENDING_CREATE,
    val lastSyncAttemptAt: Instant? = null,
    val syncError: String? = null
) {
    fun toCustomer() = com.shop.billing.data.model.Customer(
        name = name, mobile = mobile,
        totalBills = totalBills, totalSpent = totalSpent,
        pendingAmount = pendingAmount, creditAmount = creditAmount,
        createdAt = createdAt.toEpochMilli(),
        updatedAt = updatedAt.toEpochMilli(),
        deleted = deleted, version = version,
        ownerId = ownerId
    )
}
