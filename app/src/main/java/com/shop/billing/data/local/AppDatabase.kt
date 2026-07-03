package com.shop.billing.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.shop.billing.data.local.converter.Converters
import com.shop.billing.data.local.dao.CustomerDao
import com.shop.billing.data.local.dao.CustomerPaymentDao
import com.shop.billing.data.local.dao.InvestmentDao
import com.shop.billing.data.local.dao.InvoiceDao
import com.shop.billing.data.local.dao.InvoiceItemDao
import com.shop.billing.data.local.dao.ProductDao
import com.shop.billing.data.local.entity.CustomerEntity
import com.shop.billing.data.local.entity.CustomerPaymentEntity
import com.shop.billing.data.local.entity.InvestmentEntity
import com.shop.billing.data.local.entity.InvoiceEntity
import com.shop.billing.data.local.entity.InvoiceItemEntity
import com.shop.billing.data.local.entity.ProductEntity

@Database(
    entities = [
        ProductEntity::class,
        CustomerEntity::class,
        InvoiceEntity::class,
        InvoiceItemEntity::class,
        CustomerPaymentEntity::class,
        InvestmentEntity::class
    ],
    version = 8,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun customerDao(): CustomerDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun invoiceItemDao(): InvoiceItemDao
    abstract fun customerPaymentDao(): CustomerPaymentDao
    abstract fun investmentDao(): InvestmentDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS sync_operations")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP INDEX IF EXISTS idx_invoices_shop_del_created")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_invoices_shopCode_deleted_createdAt ON invoices(shopCode, deleted, createdAt)")
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN barcode TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE products ADD COLUMN stockQuantity INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE invoice_items ADD COLUMN productId TEXT NOT NULL DEFAULT ''")
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN lowStockThreshold INTEGER NOT NULL DEFAULT 10")
            }
        }
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS investments (id TEXT NOT NULL PRIMARY KEY, amount REAL NOT NULL, createdAt INTEGER NOT NULL, shopCode TEXT NOT NULL DEFAULT '')")
            }
        }
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE investments ADD COLUMN productId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE investments ADD COLUMN productName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE investments ADD COLUMN quantity INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE investments ADD COLUMN purchasePrice REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE investments ADD COLUMN sellingPriceAtPurchase REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE investments ADD COLUMN barcode TEXT NOT NULL DEFAULT ''")
            }
        }
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN buyingPrice REAL NOT NULL DEFAULT 0.0")
            }
        }
    }
}