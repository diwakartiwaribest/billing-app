package com.shop.billing.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shop.billing.data.local.entity.InvoiceItemEntity
import com.shop.billing.data.local.entity.SyncStatus
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface InvoiceItemDao {
    @Query("SELECT COUNT(*) FROM invoice_items WHERE deleted = 0 AND shopCode = :shopCode")
    fun observeCount(shopCode: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM invoice_items WHERE deleted = 1 AND shopCode = :shopCode")
    fun observeDeletedCount(shopCode: String): Flow<Int>

    @Query("SELECT * FROM invoice_items WHERE deleted = 0 AND invoiceId = :invoiceId ORDER BY itemName ASC")
    fun observeByInvoice(invoiceId: String): Flow<List<InvoiceItemEntity>>

    @Query("SELECT * FROM invoice_items WHERE deleted = 0 AND invoiceId = :invoiceId ORDER BY itemName ASC")
    suspend fun getByInvoice(invoiceId: String): List<InvoiceItemEntity>

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId ORDER BY itemName ASC")
    suspend fun getByInvoiceAll(invoiceId: String): List<InvoiceItemEntity>

    @Query("SELECT * FROM invoice_items WHERE id = :id")
    suspend fun getById(id: String): InvoiceItemEntity?

    @Query("SELECT * FROM invoice_items WHERE deleted = 0 AND shopCode = :shopCode ORDER BY invoiceId ASC")
    suspend fun getAll(shopCode: String): List<InvoiceItemEntity>

    @Query("SELECT * FROM invoice_items WHERE shopCode = :shopCode ORDER BY invoiceId ASC")
    suspend fun getAllIncludeDeleted(shopCode: String): List<InvoiceItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: InvoiceItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<InvoiceItemEntity>)

    @Query("UPDATE invoice_items SET deleted = 1, updatedAt = :updatedAt, version = version + 1, syncStatus = :syncStatus WHERE id = :id")
    suspend fun markDeleted(id: String, updatedAt: Instant, syncStatus: SyncStatus)

    @Query("UPDATE invoice_items SET deleted = :deleted, syncStatus = 'SYNCED', syncError = NULL, version = :version, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markSynced(id: String, deleted: Boolean, version: Int, updatedAt: Instant)

    @Query("UPDATE invoice_items SET syncStatus = :status, syncError = :error WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: SyncStatus, error: String? = null)

    @Query("DELETE FROM invoice_items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM invoice_items WHERE deleted = 1 AND invoiceId = :invoiceId")
    suspend fun getDeletedByInvoice(invoiceId: String): List<InvoiceItemEntity>

    @Query("DELETE FROM invoice_items WHERE id = :id AND deleted = 1")
    suspend fun hardDeleteDeletedById(id: String)

    @Query("UPDATE invoice_items SET deleted = 0, syncStatus = :syncStatus, version = version + 1, updatedAt = :updatedAt WHERE id = :id AND deleted = 1")
    suspend fun restoreDeletedById(id: String, syncStatus: com.shop.billing.data.local.entity.SyncStatus, updatedAt: Instant)

    @Query("UPDATE invoice_items SET deleted = 0, syncStatus = :syncStatus, version = version + 1, updatedAt = :updatedAt WHERE invoiceId = :invoiceId AND deleted = 1")
    suspend fun restoreDeletedByInvoice(invoiceId: String, syncStatus: com.shop.billing.data.local.entity.SyncStatus, updatedAt: Instant)

    @Query("SELECT * FROM invoice_items WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<InvoiceItemEntity>

    @Query("SELECT * FROM invoice_items WHERE syncStatus != 'SYNCED' AND shopCode = :shopCode ORDER BY updatedAt ASC")
    suspend fun getPendingSync(shopCode: String): List<InvoiceItemEntity>

    @Query("SELECT * FROM invoice_items WHERE shopCode = :shopCode AND updatedAt > :since ORDER BY updatedAt ASC")
    suspend fun getDeltaSince(shopCode: String, since: Long): List<InvoiceItemEntity>

    @Query("SELECT * FROM invoice_items ORDER BY invoiceId ASC")
    suspend fun getAllNoFilter(): List<InvoiceItemEntity>

    @Query("UPDATE invoice_items SET shopCode = :shopCode")
    suspend fun updateShopCode(shopCode: String)

    @Query("SELECT COUNT(*) FROM invoice_items WHERE syncStatus != 'SYNCED' AND shopCode = :shopCode")
    fun observePendingSyncCount(shopCode: String): Flow<Int>
}
