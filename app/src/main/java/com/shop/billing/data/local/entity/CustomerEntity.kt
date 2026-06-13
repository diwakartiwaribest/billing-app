package com.shop.billing.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val mobile: String,
    val name: String = "",
    @ColumnInfo(name = "total_bills") val totalBills: Int = 0,
    @ColumnInfo(name = "total_spent") val totalSpent: Double = 0.0,
    @ColumnInfo(name = "pending_amount") val pendingAmount: Double = 0.0,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
