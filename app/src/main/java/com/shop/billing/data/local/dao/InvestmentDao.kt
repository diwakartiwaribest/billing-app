package com.shop.billing.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.shop.billing.data.local.entity.InvestmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvestmentDao {
    @Query("SELECT * FROM investments WHERE shopCode = :shopCode ORDER BY createdAt DESC")
    fun observeAll(shopCode: String): Flow<List<InvestmentEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM investments WHERE shopCode = :shopCode")
    fun observeTotal(shopCode: String): Flow<Double>

    @Insert
    suspend fun insert(entity: InvestmentEntity)

    @Query("DELETE FROM investments WHERE id = :id")
    suspend fun deleteById(id: String)
}
