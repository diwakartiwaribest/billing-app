package com.shop.billing.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shop.billing.data.local.entity.ShopItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShopItemDao {
    @Query("SELECT * FROM shop_items ORDER BY name ASC")
    fun getAllItems(): Flow<List<ShopItemEntity>>

    @Query("SELECT * FROM shop_items WHERE id = :id")
    suspend fun getItemById(id: String): ShopItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ShopItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ShopItemEntity>)

    @Query("UPDATE shop_items SET name = :name, price = :price, category = :category WHERE id = :id")
    suspend fun updateItem(id: String, name: String, price: Double, category: String)

    @Query("DELETE FROM shop_items WHERE id = :id")
    suspend fun deleteItem(id: String)

    @Query("DELETE FROM shop_items")
    suspend fun deleteAll()
}
