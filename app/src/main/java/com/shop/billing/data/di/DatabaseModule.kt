package com.shop.billing.data.di

import android.content.Context
import androidx.room.Room
import com.shop.billing.data.local.AppDatabase
import com.shop.billing.data.local.dao.CustomerDao
import com.shop.billing.data.local.dao.CustomerPaymentDao
import com.shop.billing.data.local.dao.InvoiceDao
import com.shop.billing.data.local.dao.InvoiceItemDao
import com.shop.billing.data.local.dao.ProductDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "billing_room.db"
        ).addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3).build()
    }

    @Provides fun provideProductDao(db: AppDatabase): ProductDao = db.productDao()
    @Provides fun provideCustomerDao(db: AppDatabase): CustomerDao = db.customerDao()
    @Provides fun provideInvoiceDao(db: AppDatabase): InvoiceDao = db.invoiceDao()
    @Provides fun provideInvoiceItemDao(db: AppDatabase): InvoiceItemDao = db.invoiceItemDao()
    @Provides fun provideCustomerPaymentDao(db: AppDatabase): CustomerPaymentDao = db.customerPaymentDao()
}
