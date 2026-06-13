package com.shop.billing.ui.screens.home

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shop.billing.data.remote.SupabaseClient
import com.shop.billing.data.remote.SyncManager
import com.shop.billing.data.repository.BillRepository
import com.shop.billing.data.repository.ShopItemRepository
import com.shop.billing.util.Constants
import com.shop.billing.util.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val syncManager: SyncManager,
    private val billRepository: BillRepository,
    private val shopItemRepository: ShopItemRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _itemCount = MutableStateFlow(0)
    val itemCount: StateFlow<Int> = _itemCount

    private val _billCount = MutableStateFlow(0)
    val billCount: StateFlow<Int> = _billCount

    private val _totalSales = MutableStateFlow(0.0)
    val totalSales: StateFlow<Double> = _totalSales

    private val _customerCount = MutableStateFlow(0)
    val customerCount: StateFlow<Int> = _customerCount

    private val _shopName = MutableStateFlow("")
    val shopName: StateFlow<String> = _shopName

    private val _syncEnabled = MutableStateFlow(false)
    val syncEnabled: StateFlow<Boolean> = _syncEnabled

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    private val _syncStatus = MutableStateFlow("")
    val syncStatus: StateFlow<String> = _syncStatus

    init {
        viewModelScope.launch {
            try {
                val prefs = context.dataStore.data.first()
                _shopName.value = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_NAME)] ?: Constants.DEFAULT_SHOP_NAME
                _syncEnabled.value = prefs[booleanPreferencesKey(Constants.SETTINGS_KEY_SYNC_ENABLED)] ?: false
            } catch (_: Exception) {
                _shopName.value = Constants.DEFAULT_SHOP_NAME
            }
        }
        loadFromRoom()
        pullFromSupabase()
    }

    private fun loadFromRoom() {
        viewModelScope.launch {
            billRepository.getAllBills().collect { bills ->
                _billCount.value = bills.size
                _totalSales.value = bills.sumOf { it.totalAmount }
            }
        }
        viewModelScope.launch {
            shopItemRepository.getAllItems().collect { items ->
                _itemCount.value = items.size
            }
        }
    }

    fun syncNow() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            _syncStatus.value = "Syncing..."
            try {
                val prefs = context.dataStore.data.first()
                val shopCode = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: ""
                val url = Constants.HARDCODED_SUPABASE_URL
                val key = Constants.HARDCODED_SUPABASE_KEY
                if (shopCode.isNotBlank()) {
                    syncManager.pushAllToSupabase(url, key, shopCode)
                    syncManager.pullAllFromSupabase(url, key, shopCode)
                }
            } catch (_: Exception) {}
            _syncStatus.value = "Synced"
            _isSyncing.value = false
        }
    }

    private fun pullFromSupabase() {
        viewModelScope.launch {
            try {
                val prefs = context.dataStore.data.first()
                val shopCode = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: ""
                val url = Constants.HARDCODED_SUPABASE_URL
                val key = Constants.HARDCODED_SUPABASE_KEY
                if (shopCode.isNotBlank()) {
                    syncManager.pullAllFromSupabase(url, key, shopCode)
                }
            } catch (_: Exception) {}
        }
    }
}
