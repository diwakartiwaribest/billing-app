package com.shop.billing.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shop.billing.data.local.entity.CustomerPaymentEntity

@Dao
interface CustomerPaymentDao {
    @Query("SELECT * FROM customer_payments WHERE customer_mobile = :mobile ORDER BY created_at DESC")
    suspend fun getPaymentsByMobile(mobile: String): List<CustomerPaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: CustomerPaymentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<CustomerPaymentEntity>)

    @Query("DELETE FROM customer_payments WHERE uuid = :uuid")
    suspend fun deletePayment(uuid: String)

    @Query("DELETE FROM customer_payments")
    suspend fun deleteAll()
}
