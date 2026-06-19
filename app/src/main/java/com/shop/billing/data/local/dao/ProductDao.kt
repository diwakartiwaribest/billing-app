package com.shop.billing.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shop.billing.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT COUNT(*) FROM products WHERE deleted = 0 AND shopCode = :shopCode")
    fun observeCount(shopCode: String): Flow<Int>

    @Query("SELECT * FROM products WHERE deleted = 0 AND shopCode = :shopCode ORDER BY name ASC")
    fun observeAll(shopCode: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE deleted = 0 AND shopCode = :shopCode ORDER BY name ASC")
    suspend fun getAll(shopCode: String): List<ProductEntity>

    @Query("SELECT * FROM products WHERE shopCode = :shopCode ORDER BY name ASC")
    suspend fun getAllIncludeDeleted(shopCode: String): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<ProductEntity>)

    @Query("UPDATE products SET syncStatus = :status, syncError = :error WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: com.shop.billing.data.local.entity.SyncStatus, error: String? = null)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM products WHERE syncStatus != 'SYNCED' AND shopCode = :shopCode ORDER BY updatedAt ASC")
    suspend fun getPendingSync(shopCode: String): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<ProductEntity>

    @Query("SELECT * FROM products WHERE shopCode = :shopCode AND updatedAt > :since ORDER BY updatedAt ASC")
    suspend fun getDeltaSince(shopCode: String, since: Long): List<ProductEntity>

    @Query("SELECT * FROM products ORDER BY name ASC")
    suspend fun getAllNoFilter(): List<ProductEntity>

    @Query("UPDATE products SET shopCode = :shopCode")
    suspend fun updateShopCode(shopCode: String)
}
