package com.shop.billing.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bills")
data class BillEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "bill_number") val billNumber: String = "",
    @ColumnInfo(name = "customer_name") val customerName: String = "",
    @ColumnInfo(name = "customer_mobile") val customerMobile: String = "",
    @ColumnInfo(name = "total_amount") val totalAmount: Double = 0.0,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "created_by") val createdBy: String = "",
    @ColumnInfo(name = "payment_status") val paymentStatus: String = "paid"
)
