package com.shop.billing.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "investments")
data class InvestmentEntity(
    @PrimaryKey val id: String,
    val amount: Double,
    val createdAt: Long,
    val shopCode: String = "",
    val productId: String = "",
    val productName: String = "",
    val quantity: Int = 0,
    val purchasePrice: Double = 0.0,
    val sellingPriceAtPurchase: Double = 0.0,
    val barcode: String = ""
)
