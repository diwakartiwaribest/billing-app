package com.shop.billing.data.model

data class CustomerPayment(
    val uuid: String = java.util.UUID.randomUUID().toString(),
    val customerMobile: String,
    val amount: Double,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
