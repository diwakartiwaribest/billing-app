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

    @Query("SELECT * FROM products WHERE barcode = :barcode AND shopCode = :shopCode AND deleted = 0 LIMIT 1")
    suspend fun getByBarcode(barcode: String, shopCode: String): ProductEntity?

    @Query("SELECT * FROM products WHERE barcode = :barcode AND deleted = 0 LIMIT 1")
    suspend fun getByBarcodeAnyShop(barcode: String): ProductEntity?

    @Query("SELECT * FROM products WHERE TRIM(barcode) = :barcode AND deleted = 0 LIMIT 1")
    suspend fun getByBarcodeTrimmed(barcode: String): ProductEntity?

    @Query("UPDATE products SET stockQuantity = :quantity, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStockQuantity(id: String, quantity: Int, updatedAt: java.time.Instant = java.time.Instant.now())

    @Query("SELECT * FROM products WHERE barcode != '' AND deleted = 0 AND shopCode = :shopCode")
    fun observeBarcoded(shopCode: String): Flow<List<ProductEntity>>

    @Query("UPDATE products SET shopCode = :shopCode")
    suspend fun updateShopCode(shopCode: String)

    @Query("SELECT * FROM products WHERE deleted = 1 AND shopCode = :shopCode AND updatedAt < :beforeTimestamp ORDER BY updatedAt ASC")
    suspend fun getDeletedBeforeTimestamp(shopCode: String, beforeTimestamp: Long): List<ProductEntity>

    @Query("DELETE FROM products WHERE id = :id AND deleted = 1")
    suspend fun hardDeleteDeletedById(id: String)

    @Query("UPDATE products SET deleted = 0, syncStatus = :status, version = version + 1, updatedAt = :updatedAt WHERE id = :id AND deleted = 1")
    suspend fun restoreDeletedById(id: String, status: com.shop.billing.data.local.entity.SyncStatus, updatedAt: java.time.Instant)

    @Query("UPDATE products SET deleted = :deleted, syncStatus = 'SYNCED', syncError = NULL, version = :version, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markSynced(id: String, deleted: Boolean, version: Int, updatedAt: java.time.Instant)

    @Query("SELECT COUNT(*) FROM products WHERE deleted = 0 AND shopCode = :shopCode AND stockQuantity = 0")
    fun observeOutOfStockCount(shopCode: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM products WHERE deleted = 1 AND shopCode = :shopCode")
    fun observeDeletedCount(shopCode: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM products WHERE deleted = 0 AND shopCode = :shopCode AND stockQuantity > 0 AND stockQuantity <= lowStockThreshold")
    fun observeLowStockCount(shopCode: String): Flow<Int>

    @Query("SELECT COALESCE(SUM(buyingPrice * stockQuantity), 0) FROM products WHERE deleted = 0 AND shopCode = :shopCode")
    fun observeTotalStockValue(shopCode: String): Flow<Double>

    @Query("SELECT COALESCE(SUM(price * stockQuantity), 0) FROM products WHERE deleted = 0 AND shopCode = :shopCode")
    fun observeTotalStockMrp(shopCode: String): Flow<Double>

    @Query("SELECT category FROM products WHERE deleted = 0 AND shopCode = :shopCode AND category != ''")
    fun observeAllCategories(shopCode: String): Flow<List<String>>

    @Query("SELECT * FROM products WHERE deleted = 1 AND shopCode = :shopCode ORDER BY updatedAt DESC")
    fun observeDeleted(shopCode: String): Flow<List<ProductEntity>>

    @Query("SELECT COUNT(*) FROM products WHERE syncStatus != 'SYNCED' AND shopCode = :shopCode")
    fun observePendingSyncCount(shopCode: String): Flow<Int>
}
