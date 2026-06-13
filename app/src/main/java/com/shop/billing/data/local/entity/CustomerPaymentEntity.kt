package com.shop.billing.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customer_payments")
data class CustomerPaymentEntity(
    @PrimaryKey val uuid: String,
    @ColumnInfo(name = "customer_mobile") val customerMobile: String = "",
    val amount: Double = 0.0,
    val note: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
