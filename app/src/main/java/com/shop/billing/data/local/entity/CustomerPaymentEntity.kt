package com.shop.billing.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "customer_payments")
data class CustomerPaymentEntity(
    @PrimaryKey val uuid: String = java.util.UUID.randomUUID().toString(),
    val customerMobile: String,
    val amount: Double,
    val note: String = "",
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
    fun toCustomerPayment() = com.shop.billing.data.model.CustomerPayment(
        uuid = uuid, customerMobile = customerMobile,
        amount = amount, note = note,
        createdAt = createdAt.toEpochMilli(),
        updatedAt = updatedAt.toEpochMilli(),
        deleted = deleted, version = version,
        ownerId = ownerId
    )
}
