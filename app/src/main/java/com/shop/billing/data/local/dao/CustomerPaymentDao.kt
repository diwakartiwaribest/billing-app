package com.shop.billing.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shop.billing.data.local.entity.CustomerPaymentEntity
import com.shop.billing.data.model.MobileTotal
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerPaymentDao {
    @Query("SELECT COUNT(*) FROM customer_payments WHERE deleted = 0 AND shopCode = :shopCode")
    fun observeCount(shopCode: String): Flow<Int>

    @Query("SELECT * FROM customer_payments WHERE deleted = 0 AND shopCode = :shopCode ORDER BY createdAt DESC")
    fun observeAll(shopCode: String): Flow<List<CustomerPaymentEntity>>

    @Query("SELECT * FROM customer_payments WHERE deleted = 0 AND shopCode = :shopCode ORDER BY createdAt DESC")
    suspend fun getAll(shopCode: String): List<CustomerPaymentEntity>

    @Query("SELECT * FROM customer_payments WHERE shopCode = :shopCode ORDER BY createdAt DESC")
    suspend fun getAllIncludeDeleted(shopCode: String): List<CustomerPaymentEntity>

    @Query("SELECT * FROM customer_payments WHERE deleted = 0 AND customerMobile = :mobile ORDER BY createdAt DESC")
    suspend fun getByCustomerMobile(mobile: String): List<CustomerPaymentEntity>

    @Query("SELECT * FROM customer_payments WHERE deleted = 0 AND customerMobile = :mobile AND shopCode = :shopCode ORDER BY createdAt DESC")
    fun observeByCustomerMobile(mobile: String, shopCode: String): Flow<List<CustomerPaymentEntity>>

    @Query("SELECT customerMobile, COALESCE(SUM(amount), 0) as total FROM customer_payments WHERE deleted = 0 AND shopCode = :shopCode GROUP BY customerMobile")
    suspend fun getPaymentTotalsByMobile(shopCode: String): List<MobileTotal>

    @Query("SELECT * FROM customer_payments WHERE uuid = :uuid")
    suspend fun getByUuid(uuid: String): CustomerPaymentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CustomerPaymentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<CustomerPaymentEntity>)

    @Query("UPDATE customer_payments SET syncStatus = :status, syncError = :error WHERE uuid = :uuid")
    suspend fun updateSyncStatus(uuid: String, status: com.shop.billing.data.local.entity.SyncStatus, error: String? = null)

    @Query("DELETE FROM customer_payments WHERE uuid = :uuid")
    suspend fun deleteByUuid(uuid: String)

    @Query("SELECT * FROM customer_payments WHERE syncStatus != 'SYNCED' AND shopCode = :shopCode ORDER BY updatedAt ASC")
    suspend fun getPendingSync(shopCode: String): List<CustomerPaymentEntity>

    @Query("SELECT * FROM customer_payments WHERE uuid IN (:uuids)")
    suspend fun getByUuids(uuids: List<String>): List<CustomerPaymentEntity>

    @Query("SELECT * FROM customer_payments WHERE shopCode = :shopCode AND updatedAt > :since ORDER BY updatedAt ASC")
    suspend fun getDeltaSince(shopCode: String, since: Long): List<CustomerPaymentEntity>

    @Query("SELECT * FROM customer_payments ORDER BY createdAt DESC")
    suspend fun getAllNoFilter(): List<CustomerPaymentEntity>

    @Query("UPDATE customer_payments SET shopCode = :shopCode")
    suspend fun updateShopCode(shopCode: String)
}
