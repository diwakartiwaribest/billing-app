package com.shop.billing.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val price: Double,
    val category: String = "",
    val barcode: String = "",
    val stockQuantity: Int = 0,
    val lowStockThreshold: Int = 10,
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
    fun toShopItem() = com.shop.billing.data.model.ShopItem(
        id = id, name = name, price = price, category = category,
        barcode = barcode, stockQuantity = stockQuantity, lowStockThreshold = lowStockThreshold,
        createdAt = createdAt.toEpochMilli(), updatedAt = updatedAt.toEpochMilli(),
        deleted = deleted, version = version,
        ownerId = ownerId
    )
}
