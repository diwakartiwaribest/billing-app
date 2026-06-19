package com.shop.billing.data.model

data class BillItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val billId: String,
    val itemName: String,
    val quantity: Int,
    val unitPrice: Double,
    val subtotal: Double,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deleted: Boolean = false,
    val version: Int = 1,
    val ownerId: String = ""
)
