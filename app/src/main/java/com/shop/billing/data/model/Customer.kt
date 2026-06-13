package com.shop.billing.data.model

data class Customer(
    val name: String,
    val mobile: String,
    val totalBills: Int = 0,
    val totalSpent: Double = 0.0,
    val pendingAmount: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)
