package com.shop.billing.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shop.billing.data.local.entity.BillItemEntity

@Dao
interface BillItemDao {
    @Query("SELECT * FROM bill_items WHERE bill_id = :billId")
    suspend fun getItemsByBillId(billId: String): List<BillItemEntity>

    @Query("SELECT * FROM bill_items WHERE bill_id IN (:billIds)")
    suspend fun getItemsByBillIds(billIds: List<String>): List<BillItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: BillItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<BillItemEntity>)

    @Query("DELETE FROM bill_items WHERE bill_id = :billId")
    suspend fun deleteItemsByBillId(billId: String)

    @Query("DELETE FROM bill_items")
    suspend fun deleteAll()
}
