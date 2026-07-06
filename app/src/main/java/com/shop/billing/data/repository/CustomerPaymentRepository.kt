package com.shop.billing.data.repository

import com.shop.billing.data.local.dao.CustomerPaymentDao
import com.shop.billing.data.local.entity.CustomerPaymentEntity
import com.shop.billing.data.local.entity.SyncStatus
import com.shop.billing.data.model.CustomerPayment
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import com.shop.billing.util.Constants
import com.shop.billing.util.dataStore
import kotlinx.coroutines.flow.first

@Singleton
class CustomerPaymentRepository @Inject constructor(
    private val paymentDao: CustomerPaymentDao,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) {
    fun observeAll(shopCode: String): Flow<List<CustomerPaymentEntity>> = paymentDao.observeAll(shopCode)

    fun observeDeleted(shopCode: String): Flow<List<CustomerPaymentEntity>> = paymentDao.observeDeleted(shopCode)

    fun observeCount(shopCode: String): Flow<Int> = paymentDao.observeCount(shopCode)

    suspend fun getPaymentTotalsByMobile(shopCode: String) = paymentDao.getPaymentTotalsByMobile(shopCode)

    suspend fun getAll(shopCode: String): List<CustomerPaymentEntity> = paymentDao.getAll(shopCode)

    suspend fun getByCustomerMobile(mobile: String): List<CustomerPaymentEntity> = paymentDao.getByCustomerMobile(mobile)

    fun observeByCustomerMobile(mobile: String, shopCode: String): Flow<List<CustomerPaymentEntity>> = paymentDao.observeByCustomerMobile(mobile, shopCode)

    suspend fun create(payment: CustomerPayment, shopCode: String, ownerId: String = "") {
        val finalOwnerId = ownerId.ifBlank {
            context.dataStore.data.first()[stringPreferencesKey(Constants.SETTINGS_KEY_OWNER_ID)] ?: ""
        }
        paymentDao.upsert(CustomerPaymentEntity(
            uuid = payment.uuid, customerMobile = payment.customerMobile,
            amount = payment.amount, note = payment.note, shopCode = shopCode,
            createdAt = Instant.ofEpochMilli(payment.createdAt), updatedAt = Instant.now(),
            syncStatus = SyncStatus.PENDING_CREATE, ownerId = finalOwnerId
        ))
    }

    suspend fun softDelete(uuid: String, shopCode: String) {
        val all = paymentDao.getAll(shopCode)
        val existing = all.find { it.uuid == uuid } ?: return
        paymentDao.upsert(existing.copy(
            deleted = true, updatedAt = Instant.now(), version = existing.version + 1,
            syncStatus = SyncStatus.PENDING_DELETE
        ))
    }

    suspend fun upsertAll(entities: List<CustomerPaymentEntity>) = paymentDao.upsertAll(entities)

    suspend fun getDeletedBefore(shopCode: String, beforeTimestamp: Long): List<CustomerPaymentEntity> =
        paymentDao.getDeletedBeforeTimestamp(shopCode, beforeTimestamp)

    suspend fun hardDeleteDeleted(payment: CustomerPaymentEntity) {
        paymentDao.hardDeleteDeletedByUuid(payment.uuid)
    }

    suspend fun restoreDeleted(payment: CustomerPaymentEntity) {
        paymentDao.restoreDeletedByUuid(payment.uuid, SyncStatus.PENDING_UPDATE, java.time.Instant.now())
    }
}
