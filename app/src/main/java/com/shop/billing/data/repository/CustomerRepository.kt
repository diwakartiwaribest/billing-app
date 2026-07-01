package com.shop.billing.data.repository

import com.shop.billing.data.local.dao.CustomerDao
import com.shop.billing.data.local.entity.CustomerEntity
import com.shop.billing.data.local.entity.SyncStatus
import com.shop.billing.data.model.Customer
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
class CustomerRepository @Inject constructor(
    private val customerDao: CustomerDao,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) {
    fun observeCount(shopCode: String): Flow<Int> = customerDao.observeCount(shopCode)

    fun observeAll(shopCode: String): Flow<List<CustomerEntity>> = customerDao.observeAll(shopCode)

    suspend fun getAll(shopCode: String): List<CustomerEntity> = customerDao.getAll(shopCode)

    suspend fun getByMobile(mobile: String): CustomerEntity? = customerDao.getByMobile(mobile)

    fun observeByMobile(mobile: String): Flow<CustomerEntity?> = customerDao.observeByMobile(mobile)

    suspend fun create(customer: Customer, shopCode: String, ownerId: String = "") {
        val finalOwnerId = ownerId.ifBlank {
            context.dataStore.data.first()[stringPreferencesKey(Constants.SETTINGS_KEY_OWNER_ID)] ?: ""
        }
        customerDao.upsert(CustomerEntity(
            mobile = customer.mobile, name = customer.name, shopCode = shopCode,
            createdAt = Instant.ofEpochMilli(customer.createdAt), updatedAt = Instant.now(),
            syncStatus = SyncStatus.PENDING_CREATE, ownerId = finalOwnerId
        ))
    }

    suspend fun update(entity: CustomerEntity) {
        customerDao.upsert(entity.copy(
            updatedAt = Instant.now(), version = entity.version + 1,
            syncStatus = SyncStatus.PENDING_UPDATE
        ))
    }

    suspend fun softDelete(mobile: String) {
        val existing = customerDao.getByMobile(mobile) ?: return
        customerDao.upsert(existing.copy(
            deleted = true, updatedAt = Instant.now(), version = existing.version + 1,
            syncStatus = SyncStatus.PENDING_DELETE
        ))
    }

    suspend fun updateStats(mobile: String, totalBills: Int, totalSpent: Double, pendingAmount: Double, creditAmount: Double) {
        val existing = customerDao.getByMobile(mobile) ?: return
        customerDao.upsert(existing.copy(
            totalBills = totalBills, totalSpent = totalSpent,
            pendingAmount = pendingAmount, creditAmount = creditAmount,
            updatedAt = Instant.now(), version = existing.version + 1,
            syncStatus = SyncStatus.PENDING_UPDATE
        ))
    }

    suspend fun upsertAll(entities: List<CustomerEntity>) = customerDao.upsertAll(entities)
}
