package com.shop.billing.ui.screens.home

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shop.billing.data.remote.AppVersion
import com.shop.billing.data.remote.DownloadState
import com.shop.billing.data.remote.FirebaseClient
import com.shop.billing.data.remote.UpdateDownloader
import com.shop.billing.data.remote.UpdateNotificationManager
import com.shop.billing.data.repository.CustomerRepository
import com.shop.billing.data.repository.InvestmentRepository
import com.shop.billing.data.repository.InvoiceRepository
import com.shop.billing.data.repository.ProductRepository
import com.shop.billing.data.sync.LogEntry
import com.shop.billing.data.sync.LogType
import com.shop.billing.data.sync.SyncEngine
import com.shop.billing.data.sync.SyncService
import com.shop.billing.util.Constants
import com.shop.billing.util.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.map
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val customerRepository: CustomerRepository,
    private val invoiceRepository: InvoiceRepository,
    private val investmentRepository: InvestmentRepository,
    private val syncEngine: SyncEngine,
    private val firebaseClient: FirebaseClient,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object { private const val TAG = "HomeViewModel" }

    private val _itemCount = MutableStateFlow(0)
    val itemCount: StateFlow<Int> = _itemCount

    private val _billCount = MutableStateFlow(0)
    val billCount: StateFlow<Int> = _billCount

    private val _totalSales = MutableStateFlow(0.0)
    val totalSales: StateFlow<Double> = _totalSales

    private val _dailySales = MutableStateFlow(0.0)
    val dailySales: StateFlow<Double> = _dailySales

    private val _weeklySales = MutableStateFlow(0.0)
    val weeklySales: StateFlow<Double> = _weeklySales

    private val _monthlySales = MutableStateFlow(0.0)
    val monthlySales: StateFlow<Double> = _monthlySales

    private val _customerCount = MutableStateFlow(0)
    val customerCount: StateFlow<Int> = _customerCount

    private val _lowStockCount = MutableStateFlow(0)
    val lowStockCount: StateFlow<Int> = _lowStockCount

    private val _outOfStockCount = MutableStateFlow(0)
    val outOfStockCount: StateFlow<Int> = _outOfStockCount

    private val _totalInvestment = MutableStateFlow(0.0)
    val totalInvestment: StateFlow<Double> = _totalInvestment

    val profitLoss: StateFlow<Double> = combine(
        _totalSales, _totalInvestment
    ) { sales, investment -> sales - investment }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    private val _shopName = MutableStateFlow(Constants.DEFAULT_SHOP_NAME)
    val shopName: StateFlow<String> = _shopName

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    val logEntries: StateFlow<List<LogEntry>> get() = syncEngine.logEntries
    val showLog: StateFlow<Boolean> get() = syncEngine.showLog

    private val _updateAvailable = MutableStateFlow<AppVersion?>(null)
    val updateAvailable: StateFlow<AppVersion?> = _updateAvailable

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate

    private val _downloadState = MutableStateFlow(DownloadState())
    val downloadState: StateFlow<DownloadState> = _downloadState

    private val _isOwner = MutableStateFlow(false)
    val isOwner: StateFlow<Boolean> = _isOwner

    private val _pendingDelete = MutableStateFlow(false)
    val pendingDelete: StateFlow<Boolean> = _pendingDelete

    private val downloader = UpdateDownloader(context)

    private var currentShopCode = ""

    init {
        viewModelScope.launch {
            try {
                val prefs = context.dataStore.data.first()
                _shopName.value = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_NAME)] ?: Constants.DEFAULT_SHOP_NAME
                _isOwner.value = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ROLE)] == "owner"
                currentShopCode = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: ""

                // Migrate legacy DataStore investment to DB
                val legacyInvestment = prefs[doublePreferencesKey(Constants.SETTINGS_KEY_TOTAL_INVESTMENT)] ?: 0.0
                if (legacyInvestment > 0) {
                    withContext(Dispatchers.IO) {
                        investmentRepository.add(legacyInvestment, currentShopCode)
                    }
                    context.dataStore.edit { p ->
                        p.remove(doublePreferencesKey(Constants.SETTINGS_KEY_TOTAL_INVESTMENT))
                    }
                }

                syncEngine.addLog("App started", LogType.INFO)

                // Verify user is still a member of this shop
                if (currentShopCode.isNotBlank()) {
                    val userId = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ID)] ?: ""
                    if (userId.isNotBlank()) {
                        val role = withContext(Dispatchers.IO) {
                            firebaseClient.getUserRole(currentShopCode, userId)
                        }
                        if (role == null) {
                            syncEngine.addLog("Membership revoked, clearing data", LogType.ERROR)
                            context.dataStore.edit { p ->
                                p.clear()
                            }
                            _pendingDelete.value = true
                            return@launch
                        }
                    }
                }

                if (currentShopCode.isNotBlank()) {
                    launch {
                        productRepository.observeCount(currentShopCode).collect { count ->
                            _itemCount.value = count
                        }
                    }
                    launch {
                        invoiceRepository.observeCount(currentShopCode).collect { count ->
                            _billCount.value = count
                        }
                    }
                    launch {
                        invoiceRepository.observeTotalSales(currentShopCode).collect { total ->
                            _totalSales.value = total
                        }
                    }
                    launch {
                        val cal = java.util.Calendar.getInstance()
                        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                        cal.set(java.util.Calendar.MINUTE, 0)
                        cal.set(java.util.Calendar.SECOND, 0)
                        cal.set(java.util.Calendar.MILLISECOND, 0)
                        val dayStart = cal.timeInMillis
                        cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
                        cal.set(java.util.Calendar.MINUTE, 59)
                        cal.set(java.util.Calendar.SECOND, 59)
                        cal.set(java.util.Calendar.MILLISECOND, 999)
                        val dayEnd = cal.timeInMillis
                        invoiceRepository.observeDailySales(currentShopCode, dayStart, dayEnd).collect { total ->
                            _dailySales.value = total
                        }
                    }
                    launch {
                        val cal = java.util.Calendar.getInstance()
                        cal.add(java.util.Calendar.DAY_OF_YEAR, -7)
                        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                        cal.set(java.util.Calendar.MINUTE, 0)
                        cal.set(java.util.Calendar.SECOND, 0)
                        cal.set(java.util.Calendar.MILLISECOND, 0)
                        val weekStart = cal.timeInMillis
                        cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
                        cal.set(java.util.Calendar.MINUTE, 59)
                        cal.set(java.util.Calendar.SECOND, 59)
                        cal.set(java.util.Calendar.MILLISECOND, 999)
                        val weekEnd = cal.timeInMillis
                        invoiceRepository.observeDailySales(currentShopCode, weekStart, weekEnd).collect { total ->
                            _weeklySales.value = total
                        }
                    }
                    launch {
                        val cal = java.util.Calendar.getInstance()
                        cal.add(java.util.Calendar.DAY_OF_YEAR, -30)
                        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                        cal.set(java.util.Calendar.MINUTE, 0)
                        cal.set(java.util.Calendar.SECOND, 0)
                        cal.set(java.util.Calendar.MILLISECOND, 0)
                        val monthStart = cal.timeInMillis
                        cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
                        cal.set(java.util.Calendar.MINUTE, 59)
                        cal.set(java.util.Calendar.SECOND, 59)
                        cal.set(java.util.Calendar.MILLISECOND, 999)
                        val monthEnd = cal.timeInMillis
                        invoiceRepository.observeDailySales(currentShopCode, monthStart, monthEnd).collect { total ->
                            _monthlySales.value = total
                        }
                    }
                    launch {
                        customerRepository.observeCount(currentShopCode).collect { count ->
                            _customerCount.value = count
                        }
                    }
                    launch {
                        investmentRepository.observeTotal(currentShopCode).collect { total ->
                            _totalInvestment.value = total
                        }
                    }
                    launch {
                        productRepository.observeAll(currentShopCode).collect { entities ->
                            val items = entities.map { it.toShopItem() }
                            _lowStockCount.value = items.count { it.stockQuantity > 0 && it.stockQuantity <= it.lowStockThreshold }
                            _outOfStockCount.value = items.count { it.stockQuantity == 0 }
                        }
                    }
                    syncEngine.addLog("Connected to shop: $currentShopCode", LogType.INFO)
                    SyncService.start(context, currentShopCode)

                    launch {
                        val exportDone = prefs[booleanPreferencesKey(Constants.SETTINGS_KEY_FIREBASE_EXPORT_DONE)] ?: false
                        if (!exportDone) {
                            syncEngine.addLog("Exporting local data to Firebase...", LogType.INFO)
                            val result = syncEngine.exportAllToFirebase(currentShopCode)
                            context.dataStore.edit { p ->
                                p[booleanPreferencesKey(Constants.SETTINGS_KEY_FIREBASE_EXPORT_DONE)] = true
                            }
                            syncEngine.addLog("Firebase export: $result", LogType.SUCCESS)
                        }
                    }

                    startForegroundObserver()
                    collectDownloadState()
                    startUpdateChecker()

                    launch(Dispatchers.IO) {
                        syncEngine.addLog("Initial sync starting...", LogType.INFO)
                        syncEngine.pushPending(currentShopCode)
                        withContext(Dispatchers.Main) { syncEngine.addLog("Initial sync completed", LogType.SUCCESS) }
                    }
                    syncEngine.startRealtimeSync(currentShopCode, viewModelScope)
                } else {
                    syncEngine.addLog("Not configured: connect to a shop in Settings", LogType.INFO)
                }
            } catch (e: Exception) {
                syncEngine.addLog("Init error: ${e.message}", LogType.ERROR)
            }
        }
    }

    private fun startForegroundObserver() {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                syncEngine.addLog("App foregrounded, syncing", LogType.INFO)
                viewModelScope.launch(Dispatchers.IO) {
                    syncEngine.pushPending(currentShopCode)
                }
            }
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
    }

    fun syncNow() {
        if (_isSyncing.value || currentShopCode.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            _isSyncing.value = true
            withContext(Dispatchers.Main) { syncEngine.addLog("Manual sync started", LogType.INFO) }
            syncEngine.pushPending(currentShopCode)
            withContext(Dispatchers.Main) { syncEngine.addLog("Manual sync completed", LogType.INFO) }
            _isSyncing.value = false
        }
    }

    fun downloadUpdate() {
        val update = _updateAvailable.value ?: return
        if (_downloadState.value.isDownloading) return
        val cachedUri = downloader.getDownloadedApkUri()
        if (cachedUri != null) {
            syncEngine.addLog("Using cached APK", LogType.INFO)
            _downloadState.value = DownloadState(isComplete = true, uri = cachedUri)
            launchInstaller(cachedUri)
            return
        }
        syncEngine.addLog("Downloading update: ${update.versionName}", LogType.INFO)
        viewModelScope.launch {
            downloader.download(update.downloadUrl)
        }
    }

    fun cancelDownload() {
        syncEngine.addLog("Download cancelled", LogType.INFO)
        downloader.cancel()
    }

    fun dismissUpdate() {
        downloader.reset()
        _downloadState.value = DownloadState()
    }

    private fun collectDownloadState() {
        viewModelScope.launch {
            downloader.state.collect { state ->
                _downloadState.value = state
                if (state.isComplete && state.uri != null) {
                    launchInstaller(state.uri)
                }
            }
        }
    }

    private fun launchInstaller(uri: Uri) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            syncEngine.addLog("Failed to launch installer: ${e.message}", LogType.ERROR)
        }
    }

    private fun startUpdateChecker() {
        viewModelScope.launch {
            checkForUpdates()
            while (true) {
                delay(24 * 60 * 60 * 1000L)
                checkForUpdates()
            }
        }
    }

    private suspend fun checkForUpdates() {
        _isCheckingUpdate.value = true
        try {
            val result = withContext(Dispatchers.IO) {
                val githubUrl = "https://api.github.com/repos/diwakartiwaribest/billing-app/releases/latest"
                val conn = URL(githubUrl).openConnection() as HttpURLConnection
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.inputStream.bufferedReader().use { it.readText() }
            }
            val release = org.json.JSONObject(result)
            val tag = release.optString("tag_name", "")
            val body = release.optString("body", "")
            val versionCodeRegex = """[Vv]ersion\s*[Cc]ode[:*]*\s*(\d+)""".toRegex()
            val versionCodeMatch = versionCodeRegex.find(body)
            val latestVersionCode = versionCodeMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0L

            val currentVersionCode = try {
                context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
            } catch (e: Exception) { 0L }

            if (latestVersionCode > currentVersionCode) {
                val assets = release.optJSONArray("assets")
                var downloadUrl = ""
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        if (asset.optString("name", "").endsWith(".apk")) {
                            downloadUrl = asset.optString("browser_download_url", "")
                            break
                        }
                    }
                }
                if (downloadUrl.isNotBlank()) {
                    _updateAvailable.value = AppVersion(
                        versionCode = latestVersionCode,
                        versionName = tag,
                        downloadUrl = downloadUrl,
                        changelog = body
                    )
                    syncEngine.addLog("Update available: $tag", LogType.SUCCESS)
                    UpdateNotificationManager.showUpdateNotification(context, _updateAvailable.value!!)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "checkForUpdates failed", e)
        }
        _isCheckingUpdate.value = false
    }

    fun toggleLog() = syncEngine.toggleLog()

    fun clearLog() = syncEngine.clearLog()

    fun retryCheckForUpdates() {
        viewModelScope.launch { checkForUpdates() }
    }

    fun addToInvestment(amount: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            investmentRepository.add(amount, currentShopCode)
        }
    }
}
