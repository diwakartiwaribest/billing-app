package com.shop.billing.data.repository

import com.shop.billing.data.local.dao.ShopItemDao
import com.shop.billing.data.local.entity.ShopItemEntity
import com.shop.billing.data.model.ShopItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShopItemRepository @Inject constructor(
    private val shopItemDao: ShopItemDao
) {
    fun getAllItems(): Flow<List<ShopItem>> {
        return shopItemDao.getAllItems().map { entities ->
            entities.map { it.toModel() }
        }
    }

    suspend fun getItemById(id: String): ShopItem? {
        return shopItemDao.getItemById(id)?.toModel()
    }

    suspend fun insertItem(item: ShopItem) {
        shopItemDao.insertItem(item.toEntity())
    }

    suspend fun insertItems(items: List<ShopItem>) {
        shopItemDao.insertItems(items.map { it.toEntity() })
    }

    suspend fun updateItem(id: String, name: String, price: Double, category: String) {
        shopItemDao.updateItem(id, name, price, category)
    }

    suspend fun deleteItem(id: String) {
        shopItemDao.deleteItem(id)
    }

    suspend fun deleteAll() {
        shopItemDao.deleteAll()
    }
}

private fun ShopItemEntity.toModel() = ShopItem(
    id = id,
    name = name,
    price = price,
    category = category,
    createdAt = createdAt
)

private fun ShopItem.toEntity() = ShopItemEntity(
    id = id,
    name = name,
    price = price,
    category = category,
    createdAt = createdAt
)
