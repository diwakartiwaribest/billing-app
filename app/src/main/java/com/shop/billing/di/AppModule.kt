package com.shop.billing.di

import com.shop.billing.data.remote.SupabaseClient
import com.shop.billing.data.remote.SupabaseRealtimeClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
    fun provideSupabaseRealtimeClient(): SupabaseRealtimeClient {
        return SupabaseRealtimeClient()
    }
}
