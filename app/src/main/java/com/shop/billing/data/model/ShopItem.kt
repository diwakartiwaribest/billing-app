package com.shop.billing.data.model

data class ShopItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val price: Double,
    val category: String = "General",
    val createdAt: Long = System.currentTimeMillis()
)
