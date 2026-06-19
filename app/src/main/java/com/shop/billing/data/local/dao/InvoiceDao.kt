package com.shop.billing.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.sqlite.db.SupportSQLiteQuery
import com.shop.billing.data.local.entity.InvoiceEntity
import com.shop.billing.data.local.entity.SyncStatus
import com.shop.billing.data.model.MobileTotal
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface InvoiceDao {
    @Query("SELECT COUNT(*) FROM invoices WHERE deleted = 0 AND shopCode = :shopCode")
    fun observeCount(shopCode: String): Flow<Int>

    @Query("SELECT COALESCE(SUM(totalAmount), 0) FROM invoices WHERE deleted = 0 AND shopCode = :shopCode")
    fun observeTotalSales(shopCode: String): Flow<Double>

    @Query("SELECT * FROM invoices WHERE deleted = 0 AND shopCode = :shopCode ORDER BY createdAt DESC")
    fun observeAll(shopCode: String): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE deleted = 0 AND customerMobile = :mobile AND shopCode = :shopCode ORDER BY createdAt DESC LIMIT 500")
    fun observeByCustomerMobile(mobile: String, shopCode: String): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE deleted = 0 AND shopCode = :shopCode ORDER BY createdAt DESC")
    suspend fun getAll(shopCode: String): List<InvoiceEntity>

    @Query("SELECT * FROM invoices WHERE deleted = 0 AND shopCode = :shopCode AND createdAt >= :startDate AND createdAt <= :endDate ORDER BY createdAt DESC")
    suspend fun getByDateRange(shopCode: String, startDate: Long, endDate: Long): List<InvoiceEntity>

    @Query("SELECT * FROM invoices WHERE deleted = 0 AND shopCode = :shopCode AND createdAt >= :startDate ORDER BY createdAt DESC")
    suspend fun getByStartDate(shopCode: String, startDate: Long): List<InvoiceEntity>

    @Query("SELECT * FROM invoices WHERE deleted = 0 AND shopCode = :shopCode AND createdAt <= :endDate ORDER BY createdAt DESC")
    suspend fun getByEndDate(shopCode: String, endDate: Long): List<InvoiceEntity>

    @Query("SELECT * FROM invoices WHERE deleted = 0 AND shopCode = :shopCode ORDER BY createdAt DESC")
    fun observePagedSimple(shopCode: String): PagingSource<Int, InvoiceEntity>

    @Query("SELECT * FROM invoices WHERE deleted = 0 AND shopCode = :shopCode AND createdAt >= :startDate ORDER BY createdAt DESC")
    fun observePagedWithStart(shopCode: String, startDate: Long): PagingSource<Int, InvoiceEntity>

    @Query("SELECT * FROM invoices WHERE deleted = 0 AND shopCode = :shopCode AND createdAt <= :endDate ORDER BY createdAt DESC")
    fun observePagedWithEnd(shopCode: String, endDate: Long): PagingSource<Int, InvoiceEntity>

    @Query("SELECT * FROM invoices WHERE deleted = 0 AND shopCode = :shopCode AND createdAt >= :startDate AND createdAt <= :endDate ORDER BY createdAt DESC")
    fun observePagedWithDateRange(shopCode: String, startDate: Long, endDate: Long): PagingSource<Int, InvoiceEntity>

    @Query("SELECT * FROM invoices WHERE deleted = 0 AND shopCode = :shopCode AND customerName LIKE '%' || :customerName || '%' ORDER BY createdAt DESC")
    fun observePagedByName(shopCode: String, customerName: String): PagingSource<Int, InvoiceEntity>

    @Query("SELECT * FROM invoices WHERE deleted = 0 AND shopCode = :shopCode AND createdAt >= :startDate AND customerName LIKE '%' || :customerName || '%' ORDER BY createdAt DESC")
    fun observePagedByNameWithStart(shopCode: String, startDate: Long, customerName: String): PagingSource<Int, InvoiceEntity>

    @Query("SELECT * FROM invoices WHERE deleted = 0 AND shopCode = :shopCode AND createdAt <= :endDate AND customerName LIKE '%' || :customerName || '%' ORDER BY createdAt DESC")
    fun observePagedByNameWithEnd(shopCode: String, endDate: Long, customerName: String): PagingSource<Int, InvoiceEntity>

    @Query("SELECT * FROM invoices WHERE deleted = 0 AND shopCode = :shopCode AND createdAt >= :startDate AND createdAt <= :endDate AND customerName LIKE '%' || :customerName || '%' ORDER BY createdAt DESC")
    fun observePagedByNameWithDateRange(shopCode: String, startDate: Long, endDate: Long, customerName: String): PagingSource<Int, InvoiceEntity>

    @Query("SELECT * FROM invoices WHERE shopCode = :shopCode ORDER BY createdAt DESC")
    suspend fun getAllIncludeDeleted(shopCode: String): List<InvoiceEntity>

    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun getById(id: String): InvoiceEntity?

    @Query("SELECT * FROM invoices WHERE id = :id")
    fun observeById(id: String): Flow<InvoiceEntity?>

    @Query("SELECT * FROM invoices WHERE deleted = 0 AND customerMobile = :mobile ORDER BY createdAt DESC")
    suspend fun getByCustomerMobile(mobile: String): List<InvoiceEntity>

    @Query("SELECT customerMobile, COALESCE(SUM(totalAmount), 0) as total FROM invoices WHERE deleted = 0 AND shopCode = :shopCode AND paymentStatus = 'credit' GROUP BY customerMobile")
    suspend fun getCreditTotalsByMobile(shopCode: String): List<MobileTotal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: InvoiceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<InvoiceEntity>)

    @Query("UPDATE invoices SET deleted = 1, updatedAt = :updatedAt, version = version + 1, syncStatus = :syncStatus WHERE id = :id")
    suspend fun markDeleted(id: String, updatedAt: Instant, syncStatus: SyncStatus)

    @Query("UPDATE invoices SET syncStatus = :status, syncError = :error WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: SyncStatus, error: String? = null)

    @Query("DELETE FROM invoices WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM invoices WHERE syncStatus != 'SYNCED' AND shopCode = :shopCode ORDER BY updatedAt ASC")
    suspend fun getPendingSync(shopCode: String): List<InvoiceEntity>

    @Query("SELECT * FROM invoices WHERE shopCode = :shopCode AND updatedAt > :since ORDER BY updatedAt ASC")
    suspend fun getDeltaSince(shopCode: String, since: Long): List<InvoiceEntity>

    @Query("SELECT COUNT(*) FROM invoices WHERE deleted = 0 AND shopCode = :shopCode")
    suspend fun count(shopCode: String): Int

    @Query("SELECT * FROM invoices WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<InvoiceEntity>

    @Query("SELECT COUNT(*) FROM invoices")
    suspend fun countAll(): Int

    @Query("SELECT * FROM invoices ORDER BY createdAt DESC")
    suspend fun getAllNoFilter(): List<InvoiceEntity>

    @Query("UPDATE invoices SET shopCode = :shopCode")
    suspend fun updateShopCode(shopCode: String)
}
