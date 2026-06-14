package com.shop.billing.ui.screens.home

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shop.billing.data.remote.RealtimeChange
import com.shop.billing.data.remote.SupabaseClient
import com.shop.billing.data.remote.SupabaseRealtimeClient
import com.shop.billing.data.remote.UpdateManager
import com.shop.billing.data.remote.AppVersion
import com.shop.billing.util.Constants
import com.shop.billing.util.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class LogEntry(
    val timestamp: String,
    val message: String,
    val type: LogType = LogType.INFO
)

enum class LogType { INFO, SUCCESS, ERROR }

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val realtimeClient: SupabaseRealtimeClient,
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

    private val _logEntries = MutableStateFlow<List<LogEntry>>(emptyList())
    val logEntries: StateFlow<List<LogEntry>> = _logEntries

    private val _apiOnline = MutableStateFlow(false)
    val apiOnline: StateFlow<Boolean> = _apiOnline

    private val _websocketOnline = MutableStateFlow(false)
    val websocketOnline: StateFlow<Boolean> = _websocketOnline

    private val _showLog = MutableStateFlow(true)
    val showLog: StateFlow<Boolean> = _showLog

    private val _updateAvailable = MutableStateFlow<AppVersion?>(null)
    val updateAvailable: StateFlow<AppVersion?> = _updateAvailable

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

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
        pullFromSupabase()
        startConnectionMonitor()
        startRealtime()
        startUpdateChecker()
    }

    private fun addLog(message: String, type: LogType = LogType.INFO) {
        val entry = LogEntry(
            timestamp = timeFormat.format(Date()),
            message = message,
            type = type
        )
        _logEntries.value = (_logEntries.value + entry).takeLast(50)
    }

    fun toggleLog() {
        _showLog.value = !_showLog.value
    }

    fun clearLog() {
        _logEntries.value = emptyList()
    }

    private fun startConnectionMonitor() {
        viewModelScope.launch {
            while (true) {
                checkApiStatus()
                delay(15000)
            }
        }
    }

    private suspend fun checkApiStatus() {
        withContext(Dispatchers.IO) {
            var conn: HttpURLConnection? = null
            try {
                val prefs = context.dataStore.data.first()
                val url = (prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_URL)] ?: "")
                    .ifBlank { Constants.HARDCODED_SUPABASE_URL }
                val key = (prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_KEY)] ?: "")
                    .ifBlank { Constants.HARDCODED_SUPABASE_KEY }
                conn = URL("${url.trimEnd('/')}/rest/v1/shops?select=code&limit=1").openConnection() as HttpURLConnection
                conn.setRequestProperty("apikey", key)
                conn.setRequestProperty("Authorization", "Bearer $key")
                conn.setRequestProperty("Accept", "application/json")
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.requestMethod = "GET"
                val wasOnline = _apiOnline.value
                val responseCode = conn.responseCode
                _apiOnline.value = responseCode in 200..299
                if (!wasOnline && _apiOnline.value) {
                    addLog("API Online (HTTP $responseCode)", LogType.SUCCESS)
                } else if (wasOnline && !_apiOnline.value) {
                    addLog("API Offline (HTTP $responseCode)", LogType.ERROR)
                }
            } catch (e: Exception) {
                val wasOnline = _apiOnline.value
                _apiOnline.value = false
                if (wasOnline) {
                    addLog("API Offline", LogType.ERROR)
                }
            } finally {
                conn?.disconnect()
            }
        }
    }

    private fun startRealtime() {
        viewModelScope.launch {
            realtimeClient.connected.collect { connected ->
                val wasOnline = _websocketOnline.value
                _websocketOnline.value = connected
                if (!wasOnline && connected) {
                    addLog("Realtime connected", LogType.SUCCESS)
                } else if (wasOnline && !connected) {
                    addLog("Realtime disconnected", LogType.ERROR)
                }
            }
        }
        viewModelScope.launch {
            try {
                val prefs = context.dataStore.data.first()
                val url = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_URL)] ?: Constants.HARDCODED_SUPABASE_URL
                val key = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_KEY)] ?: Constants.HARDCODED_SUPABASE_KEY
                if (url.isNotBlank() && key.isNotBlank()) {
                    realtimeClient.connect(url, key)
                }
            } catch (e: Exception) {
                _websocketOnline.value = false
                addLog("Realtime connection failed: ${e.message}", LogType.ERROR)
            }
        }
        viewModelScope.launch {
            realtimeClient.events.collect { change ->
                addLog("Change: ${change.type} on ${change.table}", LogType.INFO)
                pullFromSupabase()
            }
        }
    }

    fun syncNow() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            _syncStatus.value = "Syncing..."
            pullFromSupabase()
            _syncStatus.value = "Synced"
            _isSyncing.value = false
        }
    }

    private fun pullFromSupabase() {
        viewModelScope.launch {
            try {
                val prefs = context.dataStore.data.first()
                val shopCode = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: ""
                val url = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_URL)] ?: Constants.HARDCODED_SUPABASE_URL
                val key = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_KEY)] ?: Constants.HARDCODED_SUPABASE_KEY
                if (shopCode.isNotBlank()) {
                    addLog("Fetching shop code: $shopCode", LogType.INFO)

                    addLog("Fetching items...", LogType.INFO)
                    val items = withContext(Dispatchers.IO) {
                        supabaseClient.pullShopItems(url, key, shopCode)
                    }
                    _itemCount.value = items.size
                    addLog("Items: ${items.size}", LogType.SUCCESS)

                    addLog("Fetching bills...", LogType.INFO)
                    val (bills, _) = withContext(Dispatchers.IO) {
                        supabaseClient.pullBills(url, key, shopCode)
                    }
                    _billCount.value = bills.size
                    _totalSales.value = bills.sumOf { it.totalAmount }
                    addLog("Bills: ${bills.size} Sales: ${_totalSales.value}", LogType.SUCCESS)

                    addLog("Fetching customers...", LogType.INFO)
                    val customers = withContext(Dispatchers.IO) {
                        supabaseClient.pullCustomers(url, key, shopCode)
                    }
                    _customerCount.value = customers.size
                    addLog("Customers: ${customers.size}", LogType.SUCCESS)
                }
            } catch (e: Exception) {
                addLog("Sync failed: ${e.message}", LogType.ERROR)
            }
        }
    }

    private fun startUpdateChecker() {
        viewModelScope.launch {
            // Check for updates on startup
            checkForUpdates()
            // Then check every 24 hours (86400000 ms)
            while (true) {
                delay(86400000)
                checkForUpdates()
            }
        }
    }

    private suspend fun checkForUpdates() {
        _isCheckingUpdate.value = true
        try {
            val updateManager = UpdateManager(context)
            val update = updateManager.checkForUpdate()
            _updateAvailable.value = update
            if (update != null) {
                addLog("Update available: ${update.versionName}", LogType.INFO)
            }
        } catch (e: Exception) {
            Log.e("UpdateCheck", "Failed to check for updates", e)
        } finally {
            _isCheckingUpdate.value = false
        }
    }

    fun retryCheckForUpdates() {
        viewModelScope.launch {
            checkForUpdates()
        }
    }

    override fun onCleared() {
        super.onCleared()
        realtimeClient.disconnect()
    }
}
