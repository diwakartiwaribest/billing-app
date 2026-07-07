package com.shop.billing.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shop.billing.data.local.entity.InvestmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvestmentDao {
    @Query("SELECT COUNT(*) FROM investments WHERE shopCode = :shopCode")
    fun observeCount(shopCode: String): Flow<Int>

    @Query("SELECT * FROM investments WHERE shopCode = :shopCode ORDER BY createdAt DESC")
    fun observeAll(shopCode: String): Flow<List<InvestmentEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM investments WHERE shopCode = :shopCode")
    fun observeTotal(shopCode: String): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: InvestmentEntity)

    @Insert
    suspend fun insert(entity: InvestmentEntity)

    @Query("SELECT * FROM investments WHERE id = :id")
    suspend fun getById(id: String): InvestmentEntity?

    @Query("DELETE FROM investments WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT purchasePrice FROM investments WHERE productId = :productId AND shopCode = :shopCode ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestPurchasePrice(productId: String, shopCode: String): Double?
}
