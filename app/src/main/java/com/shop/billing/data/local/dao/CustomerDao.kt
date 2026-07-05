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

    @Query("SELECT * FROM customers WHERE deleted = 1 AND shopCode = :shopCode AND updatedAt < :beforeTimestamp ORDER BY updatedAt ASC")
    suspend fun getDeletedBeforeTimestamp(shopCode: String, beforeTimestamp: Long): List<CustomerEntity>

    @Query("DELETE FROM customers WHERE mobile = :mobile AND deleted = 1")
    suspend fun hardDeleteDeletedByMobile(mobile: String)

    @Query("UPDATE customers SET deleted = 0, syncStatus = :status, version = version + 1, updatedAt = :updatedAt WHERE mobile = :mobile AND deleted = 1")
    suspend fun restoreDeletedByMobile(mobile: String, status: com.shop.billing.data.local.entity.SyncStatus, updatedAt: java.time.Instant)

    @Query("UPDATE customers SET deleted = :deleted, syncStatus = 'SYNCED', syncError = NULL, version = :version, updatedAt = :updatedAt WHERE mobile = :mobile")
    suspend fun markSynced(mobile: String, deleted: Boolean, version: Int, updatedAt: java.time.Instant)

    @Query("SELECT * FROM customers ORDER BY updatedAt ASC")
    suspend fun getAllNoFilter(): List<CustomerEntity>

    @Query("UPDATE customers SET shopCode = :shopCode")
    suspend fun updateShopCode(shopCode: String)

    @Query("SELECT COUNT(*) FROM customers WHERE deleted = 1 AND shopCode = :shopCode")
    fun observeDeletedCount(shopCode: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM customers WHERE syncStatus != 'SYNCED' AND shopCode = :shopCode")
    fun observePendingSyncCount(shopCode: String): Flow<Int>
}
