package com.shop.billing.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.sqlite.db.SimpleSQLiteQuery
import com.shop.billing.data.local.dao.InvoiceDao
import com.shop.billing.data.local.dao.InvoiceItemDao
import com.shop.billing.data.local.entity.InvoiceEntity
import com.shop.billing.data.local.entity.InvoiceItemEntity
import com.shop.billing.data.local.entity.SyncStatus
import com.shop.billing.data.model.Bill
import com.shop.billing.data.model.BillItem
import com.shop.billing.util.Constants
import com.shop.billing.util.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InvoiceRepository @Inject constructor(
    private val invoiceDao: InvoiceDao,
    private val invoiceItemDao: InvoiceItemDao,
    @ApplicationContext private val context: Context
) {
    fun observeCount(shopCode: String): Flow<Int> = invoiceDao.observeCount(shopCode)

    fun observeTotalSales(shopCode: String): Flow<Double> = invoiceDao.observeTotalSales(shopCode)

    fun observeDailySales(shopCode: String, dayStart: Long, dayEnd: Long): Flow<Double> = invoiceDao.observeDailySales(shopCode, dayStart, dayEnd)

    fun observeAll(shopCode: String): Flow<List<InvoiceEntity>> = invoiceDao.observeAll(shopCode)

    fun observeDeleted(shopCode: String): Flow<List<InvoiceEntity>> = invoiceDao.observeDeleted(shopCode)

    fun observeByCustomerMobile(mobile: String, shopCode: String): Flow<List<InvoiceEntity>> = invoiceDao.observeByCustomerMobile(mobile, shopCode)

    suspend fun getCreditTotalsByMobile(shopCode: String) = invoiceDao.getCreditTotalsByMobile(shopCode)

    fun observePaged(shopCode: String, startDate: Long? = null, endDate: Long? = null): Flow<PagingData<InvoiceEntity>> {
        return Pager(PagingConfig(pageSize = 50)) {
            when {
                startDate != null && endDate != null -> invoiceDao.observePagedWithDateRange(shopCode, startDate, endDate)
                startDate != null -> invoiceDao.observePagedWithStart(shopCode, startDate)
                endDate != null -> invoiceDao.observePagedWithEnd(shopCode, endDate)
                else -> invoiceDao.observePagedSimple(shopCode)
            }
        }.flow
    }

    fun observePagedWithName(shopCode: String, startDate: Long? = null, endDate: Long? = null, customerName: String = ""): Flow<PagingData<InvoiceEntity>> {
        val query = if (customerName.isBlank()) "" else customerName
        return Pager(PagingConfig(pageSize = 50)) {
            when {
                startDate != null && endDate != null -> invoiceDao.observePagedByNameWithDateRange(shopCode, startDate, endDate, query)
                startDate != null -> invoiceDao.observePagedByNameWithStart(shopCode, startDate, query)
                endDate != null -> invoiceDao.observePagedByNameWithEnd(shopCode, endDate, query)
                else -> invoiceDao.observePagedByName(shopCode, query)
            }
        }.flow
    }

    suspend fun getAll(shopCode: String): List<InvoiceEntity> = invoiceDao.getAll(shopCode)

    suspend fun getByDateRange(shopCode: String, startDate: Long?, endDate: Long?): List<InvoiceEntity> {
        return when {
            startDate != null && endDate != null -> invoiceDao.getByDateRange(shopCode, startDate, endDate)
            startDate != null -> invoiceDao.getByStartDate(shopCode, startDate)
            endDate != null -> invoiceDao.getByEndDate(shopCode, endDate)
            else -> invoiceDao.getAll(shopCode)
        }
    }

    suspend fun getById(id: String): InvoiceEntity? = invoiceDao.getById(id)

    fun observeById(id: String): Flow<InvoiceEntity?> = invoiceDao.observeById(id)

    suspend fun getByCustomerMobile(mobile: String): List<InvoiceEntity> = invoiceDao.getByCustomerMobile(mobile)

    suspend fun getItemsByInvoice(invoiceId: String): List<InvoiceItemEntity> = invoiceItemDao.getByInvoice(invoiceId)

    suspend fun create(bill: Bill, items: List<BillItem>, shopCode: String, ownerId: String = "") {
        val finalOwnerId = ownerId.ifBlank {
            context.dataStore.data.first()[stringPreferencesKey(Constants.SETTINGS_KEY_OWNER_ID)] ?: ""
        }
        invoiceDao.upsert(InvoiceEntity(
            id = bill.id, billNumber = bill.billNumber,
            customerName = bill.customerName, customerMobile = bill.customerMobile,
            totalAmount = bill.totalAmount, paymentStatus = bill.paymentStatus,
            createdBy = bill.createdBy, shopCode = shopCode,
            createdAt = Instant.ofEpochMilli(bill.createdAt), updatedAt = Instant.now(),
            syncStatus = SyncStatus.PENDING_CREATE, ownerId = finalOwnerId
        ))
        invoiceItemDao.upsertAll(items.map { item ->
            InvoiceItemEntity(
                id = item.id, invoiceId = item.billId,
                itemName = item.itemName, quantity = item.quantity,
                unitPrice = item.unitPrice, subtotal = item.subtotal,
                productId = item.productId,
                shopCode = shopCode, createdAt = Instant.ofEpochMilli(item.createdAt),
                updatedAt = Instant.now(), syncStatus = SyncStatus.PENDING_CREATE, ownerId = finalOwnerId
            )
        })
    }

    suspend fun update(entity: InvoiceEntity) {
        invoiceDao.upsert(entity.copy(
            updatedAt = Instant.now(), version = entity.version + 1,
            syncStatus = SyncStatus.PENDING_UPDATE
        ))
    }

    suspend fun softDelete(id: String, shopCode: String) {
        // Note: caller is responsible for calling syncEngine.pushPending() after softDelete().
        // If the calling coroutine is cancelled before pushPending() completes, the entity
        // stays in PENDING_DELETE state locally and will be pushed on the next pushPending() call.
        val existing = invoiceDao.getById(id)
        Log.d("InvoiceRepo", "softDelete: id=$id shopCode=$shopCode existing=${existing != null} existingShopCode=${existing?.shopCode}")
        existing ?: return
        val now = Instant.now()
        invoiceDao.markDeleted(id, now, SyncStatus.PENDING_DELETE)
        val items = invoiceItemDao.getByInvoiceAll(id)
        for (item in items) {
            invoiceItemDao.markDeleted(item.id, now, SyncStatus.PENDING_DELETE)
        }
    }

    suspend fun upsertAll(entities: List<InvoiceEntity>) = invoiceDao.upsertAll(entities)

    suspend fun count(shopCode: String): Int = invoiceDao.count(shopCode)

    suspend fun getDeletedBefore(shopCode: String, beforeTimestamp: Long): List<InvoiceEntity> =
        invoiceDao.getDeletedBeforeTimestamp(shopCode, beforeTimestamp)

    suspend fun hardDeleteDeleted(bill: InvoiceEntity) {
        invoiceDao.hardDeleteDeletedById(bill.id)
    }

    suspend fun restoreDeleted(bill: InvoiceEntity) {
        val now = java.time.Instant.now()
        invoiceDao.restoreDeletedById(bill.id, SyncStatus.PENDING_UPDATE, now)
        val items = invoiceItemDao.getByInvoiceAll(bill.id)
        items.forEach { item ->
            invoiceItemDao.restoreDeletedById(item.id, SyncStatus.PENDING_UPDATE, now)
        }
    }
}
