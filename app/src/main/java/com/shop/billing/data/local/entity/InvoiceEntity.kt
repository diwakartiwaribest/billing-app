package com.shop.billing.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "invoices",
    indices = [Index(value = ["shopCode", "deleted", "createdAt"])]
)
data class InvoiceEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val billNumber: String = "",
    val customerName: String = "",
    val customerMobile: String = "",
    val totalAmount: Double = 0.0,
    val paymentStatus: String = "paid",
    val createdBy: String = "",
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
    fun toBill() = com.shop.billing.data.model.Bill(
        id = id, billNumber = billNumber,
        customerName = customerName, customerMobile = customerMobile,
        totalAmount = totalAmount, createdAt = createdAt.toEpochMilli(),
        updatedAt = updatedAt.toEpochMilli(),
        createdBy = createdBy, paymentStatus = paymentStatus,
        deleted = deleted, version = version,
        ownerId = ownerId
    )
}
