package com.shop.billing.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "invoice_items")
data class InvoiceItemEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val invoiceId: String,
    val itemName: String,
    val quantity: Int,
    val unitPrice: Double,
    val subtotal: Double,
    val productId: String = "",
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
    fun toBillItem() = com.shop.billing.data.model.BillItem(
        id = id, billId = invoiceId,
        itemName = itemName, quantity = quantity,
        unitPrice = unitPrice, subtotal = subtotal,
        productId = productId,
        createdAt = createdAt.toEpochMilli(),
        updatedAt = updatedAt.toEpochMilli(),
        deleted = deleted, version = version,
        ownerId = ownerId
    )
}
