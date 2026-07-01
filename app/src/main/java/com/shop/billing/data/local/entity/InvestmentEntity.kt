package com.shop.billing.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "investments")
data class InvestmentEntity(
    @PrimaryKey val id: String,
    val amount: Double,
    val createdAt: Long,
    val shopCode: String = ""
)
