package com.shop.billing.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.shop.billing.data.local.dao.BillDao
import com.shop.billing.data.local.dao.BillItemDao
import com.shop.billing.data.local.dao.CustomerDao
import com.shop.billing.data.local.dao.CustomerPaymentDao
import com.shop.billing.data.local.dao.ShopItemDao
import com.shop.billing.data.local.entity.BillEntity
import com.shop.billing.data.local.entity.BillItemEntity
import com.shop.billing.data.local.entity.CustomerEntity
import com.shop.billing.data.local.entity.CustomerPaymentEntity
import com.shop.billing.data.local.entity.ShopItemEntity

@Database(
    entities = [
        BillEntity::class,
        BillItemEntity::class,
        ShopItemEntity::class,
        CustomerEntity::class,
        CustomerPaymentEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun billDao(): BillDao
    abstract fun billItemDao(): BillItemDao
    abstract fun shopItemDao(): ShopItemDao
    abstract fun customerDao(): CustomerDao
    abstract fun customerPaymentDao(): CustomerPaymentDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "billing_db"
            ).fallbackToDestructiveMigration().build()
        }
    }
}
