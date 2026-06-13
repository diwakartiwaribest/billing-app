package com.shop.billing

import android.app.Application
import com.shop.billing.data.remote.RealtimeClient
import com.shop.billing.data.remote.SyncManager
import com.shop.billing.util.Constants
import com.shop.billing.util.dataStore
import dagger.hilt.android.HiltAndroidApp
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class BillingApp : Application() {

    @Inject lateinit var syncManager: SyncManager
    @Inject lateinit var realtimeClient: RealtimeClient

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            try {
                val prefs = this@BillingApp.dataStore.data.first()
                val url = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_URL)]
                    ?: Constants.HARDCODED_SUPABASE_URL
                val key = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_KEY)]
                    ?: Constants.HARDCODED_SUPABASE_KEY
                val shopCode = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: ""

                if (shopCode.isNotBlank()) {
                    syncManager.pullAllFromSupabase(url, key, shopCode)
                    realtimeClient.startPolling(applicationScope, url, key, shopCode)
                }
            } catch (_: Exception) {
            }
        }
    }
}
