package com.shop.billing.data.repository

import com.shop.billing.data.local.dao.ProductDao
import com.shop.billing.data.local.entity.ProductEntity
import com.shop.billing.data.local.entity.SyncStatus
import com.shop.billing.data.model.ShopItem
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
class ProductRepository @Inject constructor(
    private val productDao: ProductDao,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) {
    fun observeCount(shopCode: String): Flow<Int> = productDao.observeCount(shopCode)

    fun observeAll(shopCode: String): Flow<List<ProductEntity>> = productDao.observeAll(shopCode)

    suspend fun getAll(shopCode: String): List<ProductEntity> = productDao.getAll(shopCode)

    suspend fun getById(id: String): ProductEntity? = productDao.getById(id)

    suspend fun getByBarcode(barcode: String, shopCode: String): ProductEntity? = productDao.getByBarcode(barcode, shopCode)

    suspend fun getByBarcodeAnyShop(barcode: String): ProductEntity? = productDao.getByBarcodeAnyShop(barcode)

    suspend fun getByBarcodeTrimmed(barcode: String): ProductEntity? = productDao.getByBarcodeTrimmed(barcode)

    suspend fun create(item: ShopItem, shopCode: String, ownerId: String = "") {
        val finalOwnerId = ownerId.ifBlank {
            context.dataStore.data.first()[stringPreferencesKey(Constants.SETTINGS_KEY_OWNER_ID)] ?: ""
        }
        productDao.upsert(ProductEntity(
            id = item.id, name = item.name, price = item.price, category = item.category,
            barcode = item.barcode.trim(), stockQuantity = item.stockQuantity,
            shopCode = shopCode, createdAt = Instant.ofEpochMilli(item.createdAt),
            updatedAt = Instant.now(), syncStatus = SyncStatus.PENDING_CREATE, ownerId = finalOwnerId
        ))
    }

    suspend fun updateStockQuantity(id: String, quantity: Int) {
        productDao.updateStockQuantity(id, quantity)
    }

    suspend fun decreaseStock(productId: String, quantity: Int) {
        val product = productDao.getById(productId) ?: return
        val newQty = (product.stockQuantity - quantity).coerceAtLeast(0)
        productDao.updateStockQuantity(productId, newQty)
    }

    suspend fun update(entity: ProductEntity) {
        productDao.upsert(entity.copy(
            barcode = entity.barcode.trim(),
            updatedAt = Instant.now(), version = entity.version + 1,
            syncStatus = SyncStatus.PENDING_UPDATE
        ))
    }

    suspend fun softDelete(id: String, shopCode: String) {
        val existing = productDao.getById(id) ?: return
        productDao.upsert(existing.copy(
            deleted = true, updatedAt = Instant.now(), version = existing.version + 1,
            syncStatus = SyncStatus.PENDING_DELETE
        ))
    }

    suspend fun upsertAll(entities: List<ProductEntity>) {
        productDao.upsertAll(entities.map { it.copy(barcode = it.barcode.trim()) })
    }

    fun observeOutOfStockCount(shopCode: String): Flow<Int> =
        productDao.observeOutOfStockCount(shopCode)
}
