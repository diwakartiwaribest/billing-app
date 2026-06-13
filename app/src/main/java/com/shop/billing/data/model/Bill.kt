package com.shop.billing.data.model

data class Bill(
    val id: String = java.util.UUID.randomUUID().toString(),
    val billNumber: String = "",
    val customerName: String = "",
    val customerMobile: String = "",
    val totalAmount: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "",
    val paymentStatus: String = "paid"
)
