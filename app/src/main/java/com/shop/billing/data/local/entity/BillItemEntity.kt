package com.shop.billing.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bill_items")
data class BillItemEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "bill_id") val billId: String,
    @ColumnInfo(name = "item_name") val itemName: String = "",
    val quantity: Int = 0,
    @ColumnInfo(name = "unit_price") val unitPrice: Double = 0.0,
    val subtotal: Double = 0.0
)
