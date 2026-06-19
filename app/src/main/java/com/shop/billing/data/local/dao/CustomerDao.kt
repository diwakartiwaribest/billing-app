package com.shop.billing.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shop.billing.data.local.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT COUNT(*) FROM customers WHERE deleted = 0 AND shopCode = :shopCode")
    fun observeCount(shopCode: String): Flow<Int>

    @Query("SELECT * FROM customers WHERE deleted = 0 AND shopCode = :shopCode ORDER BY name ASC")
    fun observeAll(shopCode: String): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE deleted = 0 AND shopCode = :shopCode ORDER BY name ASC")
    suspend fun getAll(shopCode: String): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE shopCode = :shopCode ORDER BY name ASC")
    suspend fun getAllIncludeDeleted(shopCode: String): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE mobile = :mobile")
    suspend fun getByMobile(mobile: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE mobile = :mobile AND deleted = 0")
    fun observeByMobile(mobile: String): Flow<CustomerEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CustomerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<CustomerEntity>)

    @Query("UPDATE customers SET syncStatus = :status, syncError = :error WHERE mobile = :mobile")
    suspend fun updateSyncStatus(mobile: String, status: com.shop.billing.data.local.entity.SyncStatus, error: String? = null)

    @Query("DELETE FROM customers WHERE mobile = :mobile")
    suspend fun deleteByMobile(mobile: String)

    @Query("SELECT * FROM customers WHERE syncStatus != 'SYNCED' AND shopCode = :shopCode ORDER BY updatedAt ASC")
    suspend fun getPendingSync(shopCode: String): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE mobile IN (:mobiles)")
    suspend fun getByMobiles(mobiles: List<String>): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE shopCode = :shopCode AND updatedAt > :since ORDER BY updatedAt ASC")
    suspend fun getDeltaSince(shopCode: String, since: Long): List<CustomerEntity>

    @Query("SELECT * FROM customers ORDER BY updatedAt ASC")
    suspend fun getAllNoFilter(): List<CustomerEntity>

    @Query("UPDATE customers SET shopCode = :shopCode")
    suspend fun updateShopCode(shopCode: String)
}
