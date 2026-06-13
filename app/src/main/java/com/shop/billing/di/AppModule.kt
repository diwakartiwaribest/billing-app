package com.shop.billing.di

import android.content.Context
import com.shop.billing.data.local.AppDatabase
import com.shop.billing.data.local.dao.BillDao
import com.shop.billing.data.local.dao.BillItemDao
import com.shop.billing.data.local.dao.CustomerDao
import com.shop.billing.data.local.dao.CustomerPaymentDao
import com.shop.billing.data.local.dao.ShopItemDao
import com.shop.billing.data.remote.RealtimeClient
import com.shop.billing.data.remote.SupabaseClient
import com.shop.billing.data.remote.SyncManager
import com.shop.billing.data.repository.BillRepository
import com.shop.billing.data.repository.ShopItemRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return SupabaseClient()
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideBillDao(db: AppDatabase): BillDao = db.billDao()

    @Provides
    @Singleton
    fun provideBillItemDao(db: AppDatabase): BillItemDao = db.billItemDao()

    @Provides
    @Singleton
    fun provideShopItemDao(db: AppDatabase): ShopItemDao = db.shopItemDao()

    @Provides
    @Singleton
    fun provideCustomerDao(db: AppDatabase): CustomerDao = db.customerDao()

    @Provides
    @Singleton
    fun provideCustomerPaymentDao(db: AppDatabase): CustomerPaymentDao = db.customerPaymentDao()

    @Provides
    @Singleton
    fun provideBillRepository(billDao: BillDao, billItemDao: BillItemDao): BillRepository {
        return BillRepository(billDao, billItemDao)
    }

    @Provides
    @Singleton
    fun provideShopItemRepository(shopItemDao: ShopItemDao): ShopItemRepository {
        return ShopItemRepository(shopItemDao)
    }

    @Provides
    @Singleton
    fun provideSyncManager(
        supabaseClient: SupabaseClient,
        billRepository: BillRepository,
        shopItemRepository: ShopItemRepository
    ): SyncManager {
        return SyncManager(supabaseClient, billRepository, shopItemRepository)
    }

    @Provides
    @Singleton
    fun provideRealtimeClient(
        supabaseClient: SupabaseClient,
        billRepository: BillRepository,
        shopItemRepository: ShopItemRepository
    ): RealtimeClient {
        return RealtimeClient(supabaseClient, billRepository, shopItemRepository)
    }
}
