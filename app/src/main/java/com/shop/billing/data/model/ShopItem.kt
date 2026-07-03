package com.shop.billing.data.model

data class ShopItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val sellingPrice: Double,
    val buyingPrice: Double = 0.0,
    val category: String = "",
    val barcode: String = "",
    val stockQuantity: Int = 0,
    val lowStockThreshold: Int = 10,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deleted: Boolean = false,
    val version: Int = 1,
    val ownerId: String = ""
)
