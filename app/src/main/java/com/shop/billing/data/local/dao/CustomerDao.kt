package com.shop.billing.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shop.billing.data.local.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE mobile = :mobile")
    suspend fun getCustomerByMobile(mobile: String): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<CustomerEntity>)

    @Query("UPDATE customers SET total_bills = :totalBills, total_spent = :totalSpent, pending_amount = :pendingAmount WHERE mobile = :mobile")
    suspend fun updateCustomerStats(mobile: String, totalBills: Int, totalSpent: Double, pendingAmount: Double)

    @Query("DELETE FROM customers WHERE mobile = :mobile")
    suspend fun deleteCustomer(mobile: String)

    @Query("DELETE FROM customers")
    suspend fun deleteAll()
}
