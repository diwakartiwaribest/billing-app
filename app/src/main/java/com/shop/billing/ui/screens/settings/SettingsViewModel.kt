package com.shop.billing.ui.screens.settings

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.shop.billing.data.local.dao.CustomerDao
import com.shop.billing.data.local.dao.CustomerPaymentDao
import com.shop.billing.data.local.dao.InvoiceDao
import com.shop.billing.data.local.dao.InvoiceItemDao
import com.shop.billing.data.local.dao.InvestmentDao
import com.shop.billing.data.local.dao.ProductDao
import com.shop.billing.data.local.entity.InvoiceEntity
import com.shop.billing.data.local.entity.InvoiceItemEntity
import com.shop.billing.data.local.entity.ProductEntity

import com.shop.billing.data.remote.AppVersion
import com.shop.billing.data.remote.DownloadState
import com.shop.billing.data.remote.FirebaseClient
import com.shop.billing.data.remote.UpdateDownloader
import com.shop.billing.data.remote.UpdateManager
import com.shop.billing.data.remote.UpdateNotificationManager
import com.shop.billing.data.sync.LogEntry
import com.shop.billing.data.sync.LogType
import com.shop.billing.data.sync.SyncEngine
import com.shop.billing.data.sync.SyncService
import com.shop.billing.ui.theme.ThemeMode
import com.shop.billing.util.Constants
import com.shop.billing.util.PdfGenerator
import com.shop.billing.util.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class DatabaseStats(
    val products: Int = 0,
    val customers: Int = 0,
    val invoices: Int = 0,
    val invoiceItems: Int = 0,
    val payments: Int = 0,
    val investments: Int = 0,
    val totalSales: Double = 0.0,
    val totalPayments: Double = 0.0,
    val creditAmount: Double = 0.0,
    val totalInvested: Double = 0.0,
    val outOfStockProducts: Int = 0,
    val deletedProducts: Int = 0,
    val deletedCustomers: Int = 0,
    val deletedInvoices: Int = 0,
    val deletedInvoiceItems: Int = 0,
    val deletedPayments: Int = 0,
    val todaySales: Double = 0.0,
    val lowStockProducts: Int = 0,
    val totalStockValue: Double = 0.0,
    val totalStockMrp: Double = 0.0,
    val categoryCount: Int = 0,
    val dbFileSizeFormatted: String = "",
    val pendingSyncItems: Int = 0,
    val avgInvoiceValue: Double = 0.0,
    val profitMarginPercent: Double = 0.0
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncEngine: SyncEngine,
    private val firebaseClient: FirebaseClient,
    private val productDao: ProductDao,
    private val customerDao: CustomerDao,
    private val invoiceDao: InvoiceDao,
    private val invoiceItemDao: InvoiceItemDao,
    private val customerPaymentDao: CustomerPaymentDao,
    private val investmentDao: InvestmentDao
) : ViewModel() {

    private var _shopSaveJob: Job? = null
    private var _userRoleListenerJob: Job? = null

    private val _templatePath = MutableStateFlow("")
    val templatePath: StateFlow<String> = _templatePath

    private val _shopName = MutableStateFlow(Constants.DEFAULT_SHOP_NAME)
    val shopName: StateFlow<String> = _shopName

    private val _shopAddress = MutableStateFlow(Constants.DEFAULT_SHOP_ADDRESS)
    val shopAddress: StateFlow<String> = _shopAddress

    private val _shopPhone = MutableStateFlow(Constants.DEFAULT_SHOP_PHONE)
    val shopPhone: StateFlow<String> = _shopPhone

    private val _logoUri = MutableStateFlow<String?>(null)
    val logoUri: StateFlow<String?> = _logoUri

    private val _backupState = MutableStateFlow<BackupState>(BackupState.Idle)
    val backupState: StateFlow<BackupState> = _backupState

    private val _purgeDays = MutableStateFlow(Constants.DEFAULT_PURGE_DAYS)
    val purgeDays: StateFlow<Int> = _purgeDays

    private val _purgeInProgress = MutableStateFlow(false)
    val purgeInProgress: StateFlow<Boolean> = _purgeInProgress

    private val _purgeResult = MutableStateFlow<SyncEngine.PurgeReport?>(null)
    val purgeResult: StateFlow<SyncEngine.PurgeReport?> = _purgeResult

    private val _restoreState = MutableStateFlow<RestoreState>(RestoreState.Idle)
    val restoreState: StateFlow<RestoreState> = _restoreState

    val backupBills = MutableStateFlow(true)
    val backupShopItems = MutableStateFlow(true)
    val backupSettings = MutableStateFlow(true)

    val restoreBills = MutableStateFlow(true)
    val restoreShopItems = MutableStateFlow(true)
    val restoreSettings = MutableStateFlow(true)

    private val _shopCode = MutableStateFlow("")
    val shopCode: StateFlow<String> = _shopCode

    private val _shopSecret = MutableStateFlow("")
    val shopSecret: StateFlow<String> = _shopSecret

    private val _exportState = MutableStateFlow<String?>(null)
    val exportState: StateFlow<String?> = _exportState

    private val _databaseStats = MutableStateFlow(DatabaseStats())
    val databaseStats: StateFlow<DatabaseStats> = _databaseStats

    private val _customCategories = MutableStateFlow<List<String>>(emptyList())

    private val _qrBitmap = MutableStateFlow<Bitmap?>(null)
    val qrBitmap: StateFlow<Bitmap?> = _qrBitmap

    private val _configQrBitmap = MutableStateFlow<Bitmap?>(null)
    val configQrBitmap: StateFlow<Bitmap?> = _configQrBitmap

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode

    private val _invoiceMessage = MutableStateFlow(Constants.DEFAULT_INVOICE_MESSAGE)
    val invoiceMessage: StateFlow<String> = _invoiceMessage

    private val _userRole = MutableStateFlow("member")
    val userRole: StateFlow<String> = _userRole

    private val _isOwner = MutableStateFlow(false)
    val isOwner: StateFlow<Boolean> = _isOwner

    private val _members = MutableStateFlow<List<FirebaseClient.ShopMember>>(emptyList())
    val members: StateFlow<List<FirebaseClient.ShopMember>> = _members

    private val _shopOwnerId = MutableStateFlow("")
    val shopOwnerId: StateFlow<String> = _shopOwnerId

    private val _memberActionState = MutableStateFlow<String?>(null)
    val memberActionState: StateFlow<String?> = _memberActionState

    private val updateManager = UpdateManager(context)
    private val updateDownloader = UpdateDownloader(context)

    private val _currentVersionName = MutableStateFlow("")
    val currentVersionName: StateFlow<String> = _currentVersionName

    private val _updateAvailable = MutableStateFlow<AppVersion?>(null)
    val updateAvailable: StateFlow<AppVersion?> = _updateAvailable

    private val _downloadState = MutableStateFlow(DownloadState())
    val downloadState: StateFlow<DownloadState> = _downloadState

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate

    private val _updateCheckError = MutableStateFlow<String?>(null)
    val updateCheckError: StateFlow<String?> = _updateCheckError

    private val _updateDismissed = MutableStateFlow(false)
    val updateDismissed: StateFlow<Boolean> = _updateDismissed

    val isUpdateDownloaded: Boolean get() = updateDownloader.getDownloadedApkUri() != null

    private var _pendingAutoDownload = false

    private val _currentUserId = MutableStateFlow("")
    val currentUserId: StateFlow<String> = _currentUserId

    companion object {
        var pendingAutoDownload = false
        var pendingDownloadUrl: String? = null
        var pendingVersionName: String? = null
    }

    val logEntries: StateFlow<List<LogEntry>> get() = syncEngine.logEntries
    val showLog: StateFlow<Boolean> get() = syncEngine.showLog

    fun toggleLog() = syncEngine.toggleLog()
    fun clearLog() = syncEngine.clearLog()

    init {
        PdfGenerator.ensureTemplateFilesExist(context)
        _templatePath.value = PdfGenerator.getTemplatesDir(context).absolutePath

        _currentVersionName.value = try {
            val pkg = context.packageManager.getPackageInfo(context.packageName, 0)
            pkg.versionName ?: ""
        } catch (_: Exception) { "" }

        collectDownloadState()

        viewModelScope.launch {
            val prefs = context.dataStore.data.first()
            val localShopCode = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: ""
            _shopCode.value = localShopCode
            _shopName.value = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_NAME)] ?: Constants.DEFAULT_SHOP_NAME
            _shopAddress.value = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_ADDRESS)] ?: Constants.DEFAULT_SHOP_ADDRESS
            _shopPhone.value = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_PHONE)] ?: Constants.DEFAULT_SHOP_PHONE
            _logoUri.value = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_LOGO)]
            _shopSecret.value = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_SECRET)] ?: ""
            _invoiceMessage.value = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_INVOICE_MESSAGE)] ?: Constants.DEFAULT_INVOICE_MESSAGE
            val storedTheme = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_THEME_MODE)] ?: "system"
            _themeMode.value = try { ThemeMode.valueOf(storedTheme.uppercase()) } catch (_: Exception) { ThemeMode.SYSTEM }
            _userRole.value = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ROLE)] ?: "member"
            _isOwner.value = _userRole.value == "owner"
            _currentUserId.value = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ID)] ?: ""

            val catsJson = prefs[stringPreferencesKey("custom_categories")] ?: "[]"
            try {
                val arr = JSONArray(catsJson)
                val list = mutableListOf<String>()
                for (i in 0 until arr.length()) list.add(arr.getString(i))
                _customCategories.value = list
            } catch (_: Exception) { _customCategories.value = emptyList() }

            if (localShopCode.isNotBlank()) {
                if (_shopSecret.value.isNotBlank()) {
                    generateQrBitmap()
                }
                loadMembers()
                startUserRoleListener(localShopCode, _currentUserId.value)
                setupDbStats(localShopCode)
            }
            checkForUpdates()
        }

        viewModelScope.launch {
            context.dataStore.data.map { prefs ->
                prefs[intPreferencesKey(Constants.SETTINGS_KEY_PURGE_DAYS)] ?: Constants.DEFAULT_PURGE_DAYS
            }.distinctUntilChanged().collect { days ->
                _purgeDays.value = days
            }
        }
    }

    fun updateShopName(name: String) {
        if (_userRole.value != "owner" && _userRole.value != "admin") return
        _shopName.value = name
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_NAME)] = name
            }
        }
        syncShopToFirebase()
    }

    fun updateShopAddress(address: String) {
        if (_userRole.value != "owner" && _userRole.value != "admin") return
        _shopAddress.value = address
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_ADDRESS)] = address
            }
        }
        syncShopToFirebase()
    }

    fun updateShopPhone(phone: String) {
        if (_userRole.value != "owner" && _userRole.value != "admin") return
        _shopPhone.value = phone
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_PHONE)] = phone
            }
        }
        syncShopToFirebase()
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[stringPreferencesKey(Constants.SETTINGS_KEY_THEME_MODE)] = mode.name.lowercase()
            }
        }
    }

    fun updateInvoiceMessage(message: String) {
        if (_userRole.value != "owner" && _userRole.value != "admin") return
        _invoiceMessage.value = message
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[stringPreferencesKey(Constants.SETTINGS_KEY_INVOICE_MESSAGE)] = message
            }
        }
        syncShopToFirebase()
    }

    fun saveLogo(uri: Uri) {
        if (_userRole.value != "owner" && _userRole.value != "admin") return
        viewModelScope.launch {
            val base64 = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) { }

                try {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    if (bytes == null) {
                        Log.e("SettingsVM", "Could not read URI: $uri")
                        return@withContext null
                    }
                    val head = String(bytes, 0, minOf(bytes.size, 200)).lowercase()
                    if ("<svg" in head) {
                        Log.d("SettingsVM", "Storing raw SVG: ${bytes.size} bytes")
                        Base64.encodeToString(bytes, Base64.NO_WRAP)
                    } else {
                        val bitmap = decodeImage(bytes)
                        if (bitmap == null) {
                            Log.e("SettingsVM", "Failed to decode raster image")
                            return@withContext null
                        }
                        val resized = resizeBitmap(bitmap, 800)
                        val baos = ByteArrayOutputStream()
                        resized.compress(Bitmap.CompressFormat.PNG, 100, baos)
                        Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                    }
                } catch (e: Exception) {
                    Log.e("SettingsVM", "Failed to save logo", e)
                    null
                }
            }
            if (base64 != null) {
                _logoUri.value = base64
                context.dataStore.edit { prefs ->
                    prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_LOGO)] = base64
                }
                syncShopToFirebase()
            }
        }
    }

    private fun decodeImage(bytes: ByteArray): Bitmap? {
        val head = String(bytes, 0, minOf(bytes.size, 200)).lowercase()
        Log.d("SettingsVM", "Decoding ${bytes.size} bytes, head: ${String(bytes, 0, minOf(bytes.size, 80))}")

        if ("<svg" in head) {
            try {
                val svg = com.caverock.androidsvg.SVG.getFromInputStream(bytes.inputStream())
                val w = (svg.documentWidth.takeIf { it > 0 } ?: 800f).toInt()
                val h = (svg.documentHeight.takeIf { it > 0 } ?: 800f).toInt()
                val scale = 800f / maxOf(w, h)
                val sw = (w * scale).toInt().coerceAtLeast(400)
                val sh = (h * scale).toInt().coerceAtLeast(400)
                val bmp = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888)
                svg.renderToCanvas(android.graphics.Canvas(bmp))
                Log.d("SettingsVM", "SVG decoded OK: ${sw}x${sh}")
                return bmp
            } catch (e: Exception) {
                Log.e("SettingsVM", "SVG decode error: ${e.javaClass.name}: ${e.message}")
            }
        }

        try {
            val bf = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bf != null) {
                Log.d("SettingsVM", "BitmapFactory decoded OK: ${bf.width}x${bf.height}")
                return bf
            }
        } catch (e: Exception) {
            Log.e("SettingsVM", "BitmapFactory error: ${e.message}")
        }

        if (android.os.Build.VERSION.SDK_INT >= 28) {
            try {
                val source = android.graphics.ImageDecoder.createSource(bytes)
                val bmp = android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                    decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                }
                Log.d("SettingsVM", "ImageDecoder decoded OK: ${bmp.width}x${bmp.height}")
                return bmp
            } catch (e: Exception) {
                Log.e("SettingsVM", "ImageDecoder error: ${e.message}")
            }
        }

        Log.e("SettingsVM", "All decoders failed")
        return null
    }

    fun removeLogo() {
        if (_userRole.value != "owner" && _userRole.value != "admin") return
        _logoUri.value = null
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs.remove(stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_LOGO))
            }
        }
        syncShopToFirebase()
    }

    private fun resizeBitmap(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDim && h <= maxDim) return bitmap
        val ratio = minOf(maxDim.toFloat() / w, maxDim.toFloat() / h)
        return Bitmap.createScaledBitmap(bitmap, (w * ratio).toInt(), (h * ratio).toInt(), true)
    }

    fun backupData(destinationUri: Uri) {
        viewModelScope.launch {
            _backupState.value = BackupState.InProgress
            withContext(Dispatchers.IO) {
                try {
                    val timestamp = SimpleDateFormat("ddMMyyyy_HHmmss", Locale.US).format(Date())
                    val backupFileName = "billing_backup_$timestamp"

                    val parentDir = DocumentFile.fromTreeUri(context, destinationUri)
                        ?: throw Exception("Could not access selected folder")

                    val backupFile = parentDir.createFile("application/zip", backupFileName)
                        ?: throw Exception("Could not create backup file")

                    val root = JSONObject()

                    val invoices = invoiceDao.getAll(_shopCode.value)
                    val invoiceItems = invoiceItemDao.getAll(_shopCode.value)
                    val products = productDao.getAll(_shopCode.value)

                    if (backupBills.value) {
                        val billsArray = JSONArray()
                        val grouped = invoiceItems.groupBy { it.invoiceId }
                        for (bill in invoices) {
                            val items = grouped[bill.id] ?: emptyList()
                            val itemsArray = JSONArray()
                            for (item in items) {
                                itemsArray.put(JSONObject().apply {
                                    put("id", item.id)
                                    put("billId", item.invoiceId)
                                    put("itemName", item.itemName)
                                    put("quantity", item.quantity)
                                    put("unitPrice", item.unitPrice)
                                    put("subtotal", item.subtotal)
                                })
                            }
                            billsArray.put(JSONObject().apply {
                                put("id", bill.id)
                                put("billNumber", bill.billNumber)
                                put("customerName", bill.customerName)
                                put("customerMobile", bill.customerMobile)
                                put("totalAmount", bill.totalAmount)
                                put("createdAt", bill.createdAt.toEpochMilli())
                                put("createdBy", bill.createdBy)
                                put("items", itemsArray)
                            })
                        }
                        root.put("bills", billsArray)
                    }

                    if (backupShopItems.value) {
                        val itemsArray = JSONArray()
                        for (item in products) {
                            itemsArray.put(JSONObject().apply {
                                put("id", item.id)
                                put("name", item.name)
                                put("sellingPrice", item.sellingPrice)
                                put("buyingPrice", item.buyingPrice)
                                put("category", item.category)
                                put("createdAt", item.createdAt.toEpochMilli())
                            })
                        }
                        root.put("shop_items", itemsArray)
                    }

                    if (backupSettings.value) {
                        val prefs = context.dataStore.data.first()
                        val settingsObj = JSONObject().apply {
                            put(Constants.SETTINGS_KEY_SHOP_NAME, prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_NAME)] ?: "")
                            put(Constants.SETTINGS_KEY_SHOP_ADDRESS, prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_ADDRESS)] ?: "")
                            put(Constants.SETTINGS_KEY_SHOP_PHONE, prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_PHONE)] ?: "")
                            put(Constants.SETTINGS_KEY_SHOP_LOGO, prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_LOGO)] ?: "")
                            put(Constants.SETTINGS_KEY_SHOP_CODE, prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: "")
                            put(Constants.SETTINGS_KEY_SHOP_SECRET, prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_SECRET)] ?: "")
                        }
                        root.put("settings", settingsObj)
                    }

                    context.contentResolver.openOutputStream(backupFile.uri)?.use { output ->
                        output.write(root.toString(2).toByteArray())
                    } ?: throw Exception("Could not write backup file")

                    _backupState.value = BackupState.Success(backupFileName)
                } catch (e: Exception) {
                    Log.e("SettingsVM", "Backup failed", e)
                    _backupState.value = BackupState.Error(e.message ?: "Backup failed")
                }
            }
        }
    }

    fun restoreData(sourceUri: Uri) {
        viewModelScope.launch {
            _restoreState.value = RestoreState.InProgress
            withContext(Dispatchers.IO) {
                try {
                    val bytes = context.contentResolver.openInputStream(sourceUri)?.use {
                        it.readBytes()
                    } ?: throw Exception("Could not read backup file")

                    val root = JSONObject(String(bytes))
                    var billsRestored = 0
                    var itemsRestored = 0
                    var shopItemsRestored = 0

                    if (restoreBills.value && root.has("bills")) {
                        val billsArray = root.getJSONArray("bills")
                        for (i in 0 until billsArray.length()) {
                            val billObj = billsArray.getJSONObject(i)
                            val items = if (billObj.has("items")) {
                                val itemsArray = billObj.getJSONArray("items")
                                (0 until itemsArray.length()).map { j ->
                                    val itemObj = itemsArray.getJSONObject(j)
                                    InvoiceItemEntity(
                                        id = itemObj.getString("id"),
                                        invoiceId = itemObj.getString("billId"),
                                        itemName = itemObj.getString("itemName"),
                                        quantity = itemObj.getInt("quantity"),
                                        unitPrice = itemObj.getDouble("unitPrice"),
                                        subtotal = itemObj.getDouble("subtotal"),
                                        shopCode = _shopCode.value
                                    )
                                }
                            } else emptyList()

                            val bill = InvoiceEntity(
                                id = billObj.getString("id"),
                                billNumber = billObj.getString("billNumber"),
                                customerName = billObj.optString("customerName", ""),
                                customerMobile = billObj.optString("customerMobile", ""),
                                totalAmount = billObj.getDouble("totalAmount"),
                                createdAt = Instant.ofEpochMilli(billObj.getLong("createdAt")),
                                createdBy = billObj.optString("createdBy", ""),
                                shopCode = _shopCode.value
                            )
                            invoiceDao.upsert(bill)
                            for (item in items) {
                                invoiceItemDao.upsert(item)
                            }
                            billsRestored++
                            itemsRestored += items.size
                        }
                    }

                    if (restoreShopItems.value && root.has("shop_items")) {
                        val itemsArray = root.getJSONArray("shop_items")
                        for (i in 0 until itemsArray.length()) {
                            val itemObj = itemsArray.getJSONObject(i)
                            val product = ProductEntity(
                                id = itemObj.getString("id"),
                                name = itemObj.getString("name"),
                                sellingPrice = itemObj.optDouble("sellingPrice", itemObj.optDouble("price", 0.0)),
                                buyingPrice = itemObj.optDouble("buyingPrice", 0.0),
                                category = itemObj.optString("category", ""),
                                shopCode = _shopCode.value,
                                createdAt = Instant.ofEpochMilli(itemObj.getLong("createdAt"))
                            )
                            productDao.upsert(product)
                            shopItemsRestored++
                        }
                    }

                    if (restoreSettings.value && root.has("settings")) {
                        val settingsObj = root.getJSONObject("settings")
                        context.dataStore.edit { prefs ->
                            if (settingsObj.has(Constants.SETTINGS_KEY_SHOP_NAME)) {
                                prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_NAME)] = settingsObj.getString(Constants.SETTINGS_KEY_SHOP_NAME)
                            }
                            if (settingsObj.has(Constants.SETTINGS_KEY_SHOP_ADDRESS)) {
                                prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_ADDRESS)] = settingsObj.getString(Constants.SETTINGS_KEY_SHOP_ADDRESS)
                            }
                            if (settingsObj.has(Constants.SETTINGS_KEY_SHOP_PHONE)) {
                                prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_PHONE)] = settingsObj.getString(Constants.SETTINGS_KEY_SHOP_PHONE)
                            }
                            if (settingsObj.has(Constants.SETTINGS_KEY_SHOP_LOGO)) {
                                prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_LOGO)] = settingsObj.getString(Constants.SETTINGS_KEY_SHOP_LOGO)
                            }
                            if (settingsObj.has(Constants.SETTINGS_KEY_SHOP_CODE)) {
                                prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] = settingsObj.getString(Constants.SETTINGS_KEY_SHOP_CODE)
                            }
                            if (settingsObj.has(Constants.SETTINGS_KEY_SHOP_SECRET)) {
                                prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_SECRET)] = settingsObj.getString(Constants.SETTINGS_KEY_SHOP_SECRET)
                            }
                        }
                        val s = settingsObj
                        if (s.has(Constants.SETTINGS_KEY_SHOP_CODE)) _shopCode.value = s.getString(Constants.SETTINGS_KEY_SHOP_CODE)
                        if (s.has(Constants.SETTINGS_KEY_SHOP_SECRET)) _shopSecret.value = s.getString(Constants.SETTINGS_KEY_SHOP_SECRET)
                        if (s.has(Constants.SETTINGS_KEY_SHOP_NAME)) _shopName.value = s.getString(Constants.SETTINGS_KEY_SHOP_NAME)
                        if (s.has(Constants.SETTINGS_KEY_SHOP_ADDRESS)) _shopAddress.value = s.getString(Constants.SETTINGS_KEY_SHOP_ADDRESS)
                        if (s.has(Constants.SETTINGS_KEY_SHOP_PHONE)) _shopPhone.value = s.getString(Constants.SETTINGS_KEY_SHOP_PHONE)
                        if (s.has(Constants.SETTINGS_KEY_SHOP_LOGO)) _logoUri.value = s.getString(Constants.SETTINGS_KEY_SHOP_LOGO)
                    }

                    val summary = buildString {
                        if (restoreBills.value) append("$billsRestored bills, $itemsRestored items")
                        if (restoreShopItems.value) {
                            if (isNotEmpty()) append(", ")
                            append("$shopItemsRestored shop items")
                        }
                        if (restoreSettings.value) {
                            if (isNotEmpty()) append(", ")
                            append("settings")
                        }
                    }

                    _restoreState.value = RestoreState.Success(summary)
                } catch (e: Exception) {
                    Log.e("SettingsVM", "Restore failed", e)
                    _restoreState.value = RestoreState.Error(e.message ?: "Restore failed")
                }
            }
        }
    }

    fun resetBackupState() {
        _backupState.value = BackupState.Idle
    }

    fun resetRestoreState() {
        _restoreState.value = RestoreState.Idle
    }

    fun updateShopCode(code: String) {
        _shopCode.value = code
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] = code
            }
        }
    }

    fun updateShopSecret(secret: String) {
        _shopSecret.value = secret
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_SECRET)] = secret
            }
            if (_shopCode.value.isNotBlank() && secret.isNotBlank()) {
                generateQrBitmap()
            }
        }
    }

    private fun syncShopToFirebase() {
        val code = _shopCode.value
        if (code.isBlank()) return
        _shopSaveJob?.cancel()
        _shopSaveJob = viewModelScope.launch {
            delay(500)
            SyncEngine.localShopInfoEditTimestamp = System.currentTimeMillis()
            withContext(Dispatchers.IO) {
                firebaseClient.updateShopInfo(
                    shopCode = code,
                    name = _shopName.value,
                    address = _shopAddress.value,
                    phone = _shopPhone.value,
                    logo = _logoUri.value,
                    invoiceMessage = _invoiceMessage.value
                )
            }
        }
    }

    fun exportToFirebase() {
        val code = _shopCode.value
        if (code.isBlank()) {
            _exportState.value = "No shop code set"
            return
        }
        _exportState.value = "Exporting..."
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    firebaseClient.updateShopInfo(
                        shopCode = code,
                        name = _shopName.value,
                        address = _shopAddress.value,
                        phone = _shopPhone.value,
                        logo = _logoUri.value,
                        invoiceMessage = _invoiceMessage.value
                    )
                }
                val result = withContext(Dispatchers.IO) {
                    syncEngine.exportAllToFirebase(code)
                }
                _exportState.value = result
                context.dataStore.edit { prefs ->
                    prefs[booleanPreferencesKey(Constants.SETTINGS_KEY_FIREBASE_EXPORT_DONE)] = true
                }
            } catch (e: Exception) {
                Log.e("SettingsVM", "export failed", e)
                _exportState.value = "Error: ${e.message?.take(100)}"
            }
        }
    }

    fun updatePurgeDays(days: Int) {
        if (_userRole.value != "owner" && _userRole.value != "admin") return
        val clamped = days.coerceIn(Constants.MIN_PURGE_DAYS, Constants.MAX_PURGE_DAYS)
        _purgeDays.value = clamped
        val code = _shopCode.value
        if (code.isBlank()) return
        SyncEngine.localShopInfoEditTimestamp = System.currentTimeMillis()
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                firebaseClient.updatePurgeDays(code, clamped)
            }
        }
    }

    fun purgeNow() {
        if (_userRole.value != "owner" && _userRole.value != "admin") {
            _purgeResult.value = SyncEngine.PurgeReport(0, 0, 0, 0, 0)
            return
        }
        if (_purgeInProgress.value) return
        _purgeInProgress.value = true
        viewModelScope.launch {
            try {
                val code = _shopCode.value
                if (code.isBlank()) {
                    _purgeInProgress.value = false
                    _purgeResult.value = SyncEngine.PurgeReport(0, 0, 0, 0, 0)
                    return@launch
                }
                val report = withContext(Dispatchers.IO) {
                    syncEngine.purgeAllNow(code)
                }
                _purgeResult.value = report
            } catch (e: Exception) {
                Log.e("SettingsVM", "purgeNow failed", e)
                _purgeResult.value = SyncEngine.PurgeReport(0, 0, 0, 0, 0)
            } finally {
                _purgeInProgress.value = false
            }
        }
    }

    fun clearPurgeResult() {
        _purgeResult.value = null
    }

    fun leaveShop() {
        SyncService.stop(context)
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs.remove(stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE))
                prefs.remove(stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_SECRET))
                prefs.remove(stringPreferencesKey(Constants.SETTINGS_KEY_USER_ROLE))
            }
            _shopCode.value = ""
            _shopSecret.value = ""
            _userRole.value = "member"
            _isOwner.value = false
        }
    }

    private fun setupDbStats(code: String) {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis
        val todayEnd = todayStart + 86400000L

        viewModelScope.launch {
            combine(
                listOf<Flow<*>>(
                    productDao.observeCount(code),
                    customerDao.observeCount(code),
                    invoiceDao.observeCount(code),
                    invoiceItemDao.observeCount(code),
                    customerPaymentDao.observeCount(code),
                    investmentDao.observeCount(code),
                    invoiceDao.observeTotalSales(code),
                    customerPaymentDao.observeTotal(code),
                    invoiceDao.observeCreditTotal(code),
                    investmentDao.observeTotal(code),
                    productDao.observeOutOfStockCount(code),
                    productDao.observeDeletedCount(code),
                    customerDao.observeDeletedCount(code),
                    invoiceDao.observeDeletedCount(code),
                    invoiceItemDao.observeDeletedCount(code),
                    customerPaymentDao.observeDeletedCount(code),
                    invoiceDao.observeDailySales(code, todayStart, todayEnd),
                    productDao.observeLowStockCount(code),
                    productDao.observeTotalStockValue(code),
                    productDao.observeTotalStockMrp(code),
                    productDao.observeAllCategories(code),
                    productDao.observePendingSyncCount(code),
                    customerDao.observePendingSyncCount(code),
                    invoiceDao.observePendingSyncCount(code),
                    invoiceItemDao.observePendingSyncCount(code),
                    customerPaymentDao.observePendingSyncCount(code),
                    _customCategories
                )
            ) { array ->
                val inv = array[2] as Int
                val sales = array[6] as Double
                val invested = array[9] as Double
                val categories = array[20] as List<*>
                val customCats = array[26] as List<*>
                val allCats = categories.map { it.toString().trim() } + customCats.map { it.toString().trim() }
                val distinctCats = allCats.distinct().filter { it.isNotEmpty() }
                _databaseStats.value = DatabaseStats(
                    products = array[0] as Int,
                    customers = array[1] as Int,
                    invoices = inv,
                    invoiceItems = array[3] as Int,
                    payments = array[4] as Int,
                    investments = array[5] as Int,
                    totalSales = sales,
                    totalPayments = array[7] as Double,
                    creditAmount = array[8] as Double,
                    totalInvested = invested,
                    outOfStockProducts = array[10] as Int,
                    deletedProducts = array[11] as Int,
                    deletedCustomers = array[12] as Int,
                    deletedInvoices = array[13] as Int,
                    deletedInvoiceItems = array[14] as Int,
                    deletedPayments = array[15] as Int,
                    todaySales = array[16] as Double,
                    lowStockProducts = array[17] as Int,
                    totalStockValue = array[18] as Double,
                    totalStockMrp = array[19] as Double,
                    categoryCount = distinctCats.size,
                    pendingSyncItems = (array[21] as Int) + (array[22] as Int) + (array[23] as Int) + (array[24] as Int) + (array[25] as Int),
                    avgInvoiceValue = if (inv > 0) sales / inv else 0.0,
                    profitMarginPercent = if (sales > 0) (sales - invested) / sales * 100.0 else 0.0,
                    dbFileSizeFormatted = try {
                        val dbFile = context.getDatabasePath("billing_room.db")
                        if (dbFile.exists()) {
                            val bytes = dbFile.length()
                            if (bytes < 1024) "$bytes B"
                            else if (bytes < 1048576) "${"%,.0f".format(bytes / 1024.0)} KB"
                            else "${"%,.1f".format(bytes / 1048576.0)} MB"
                        } else ""
                    } catch (_: Exception) { "" },
                )
            }.collect { }
        }
    }

    private suspend fun refreshMembers(code: String) {
        // Sync current user's role directly from Firestore (independent of memberIds)
        val uid = context.dataStore.data.first()[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ID)] ?: ""
        if (uid.isNotBlank()) {
            val remoteRole = withContext(Dispatchers.IO) {
                firebaseClient.getUserRole(code, uid)
            }
            if (remoteRole != null && remoteRole != _userRole.value) {
                context.dataStore.edit { prefs ->
                    prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ROLE)] = remoteRole
                }
                _userRole.value = remoteRole
                _isOwner.value = remoteRole == "owner"
            }
        }

        // Migrate shop if memberIds doesn't exist yet
        withContext(Dispatchers.IO) {
            firebaseClient.migrateShopMemberIds(code)
        }

        var list = withContext(Dispatchers.IO) {
            firebaseClient.getShopMembers(code)
        }
        if (list.isEmpty() && _isOwner.value) {
            val userId = context.dataStore.data.first()[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ID)] ?: ""
            val email = context.dataStore.data.first()[stringPreferencesKey(Constants.SETTINGS_KEY_USER_EMAIL)] ?: ""
            if (userId.isNotBlank()) {
                withContext(Dispatchers.IO) {
                    firebaseClient.addUserToShop(code, userId, "owner", email)
                }
                list = withContext(Dispatchers.IO) {
                    firebaseClient.getShopMembers(code)
                }
            }
        }
        // Fill in blank emails for current user
        val currentUserId = context.dataStore.data.first()[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ID)] ?: ""
        val currentEmail = context.dataStore.data.first()[stringPreferencesKey(Constants.SETTINGS_KEY_USER_EMAIL)] ?: ""
        if (currentEmail.isNotBlank()) {
            val needsUpdate = list.any { it.userId == currentUserId && it.email.isBlank() }
            if (needsUpdate) {
                withContext(Dispatchers.IO) {
                    firebaseClient.updateMemberEmail(code, currentUserId, currentEmail)
                }
                list = withContext(Dispatchers.IO) {
                    firebaseClient.getShopMembers(code)
                }
            }
        }
        _members.value = list

        // Sync current user's role from Firestore to local DataStore
        val remoteMember = list.firstOrNull { it.userId == currentUserId }
        if (remoteMember != null && remoteMember.role != _userRole.value) {
            context.dataStore.edit { prefs ->
                prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ROLE)] = remoteMember.role
            }
            _userRole.value = remoteMember.role
            _isOwner.value = remoteMember.role == "owner"
        }

        // Load shop ownerId
        val info = withContext(Dispatchers.IO) {
            firebaseClient.getShopInfo(code)
        }
        _shopOwnerId.value = info["ownerId"]?.toString() ?: ""
    }

    fun loadMembers() {
        val code = _shopCode.value
        if (code.isBlank()) return
        viewModelScope.launch {
            refreshMembers(code)
        }
    }

    fun updateMemberRole(userId: String, newRole: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                firebaseClient.updateMemberRole(_shopCode.value, userId, newRole)
            }
            val code = _shopCode.value
            if (code.isNotBlank()) {
                refreshMembers(code)
            }
            _memberActionState.value = "Role updated to $newRole"
        }
    }

    fun removeMember(userId: String) {
        viewModelScope.launch {
            val isRemovingFirstOwner = userId == _shopOwnerId.value

            withContext(Dispatchers.IO) {
                firebaseClient.removeMember(_shopCode.value, userId)
            }

            // If first owner leaves, promote the next owner to first owner
            if (isRemovingFirstOwner) {
                val remaining = withContext(Dispatchers.IO) {
                    firebaseClient.getShopMembers(_shopCode.value)
                }
                val nextOwner = remaining.firstOrNull { it.role == "owner" }
                    ?: remaining.firstOrNull()
                if (nextOwner != null) {
                    withContext(Dispatchers.IO) {
                        firebaseClient.transferOwnership(_shopCode.value, userId, nextOwner.userId)
                    }
                    val currentUserId = context.dataStore.data.first()[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ID)] ?: ""
                    if (currentUserId == userId) {
                        context.dataStore.edit { prefs ->
                            prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ROLE)] = "member"
                        }
                        _userRole.value = "member"
                        _isOwner.value = false
                    }
                }
            }

            loadMembers()
            _memberActionState.value = "Member removed"
        }
    }

    fun transferOwnership(newOwnerId: String) {
        viewModelScope.launch {
            val currentUserId = context.dataStore.data.first()[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ID)] ?: ""
            withContext(Dispatchers.IO) {
                firebaseClient.transferOwnership(_shopCode.value, currentUserId, newOwnerId)
            }
            _shopOwnerId.value = newOwnerId
            context.dataStore.edit { prefs ->
                prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ROLE)] = "admin"
            }
            _userRole.value = "admin"
            _isOwner.value = false
            loadMembers()
            _memberActionState.value = "Ownership transferred"
        }
    }

    fun resetMemberActionState() {
        _memberActionState.value = null
    }

    fun autoCheckAndDownload() {
        val url = pendingDownloadUrl
        if (url != null) {
            pendingDownloadUrl = null
            val name = pendingVersionName ?: ""
            pendingVersionName = null
            _updateAvailable.value = AppVersion(
                versionCode = 0,
                versionName = name,
                downloadUrl = url,
                changelog = ""
            )
            pendingAutoDownload = false
            downloadUpdate()
            return
        }
        val existing = _updateAvailable.value
        if (existing != null) {
            downloadUpdate()
            return
        }
        if (_isCheckingUpdate.value) {
            _pendingAutoDownload = true
        } else {
            _pendingAutoDownload = true
            checkForUpdates()
        }
    }

    fun checkForUpdates() {
        if (_isCheckingUpdate.value) return
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            _updateCheckError.value = null
            _updateDismissed.value = false
            try {
                val result = withContext(Dispatchers.IO) {
                    updateManager.checkForUpdate()
                }
                _updateAvailable.value = result
                if (result != null) {
                    _updateCheckError.value = null
                    try {
                        UpdateNotificationManager.showUpdateNotification(context, result)
                    } catch (e: Exception) {
                        android.util.Log.e("SettingsVM", "Failed to show update notification", e)
                    }
                    if (_pendingAutoDownload) {
                        _pendingAutoDownload = false
                        downloadUpdate()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsVM", "checkForUpdates failed", e)
                _updateCheckError.value = "Check failed: ${e.message}"
            }
            _isCheckingUpdate.value = false
        }
    }

    fun downloadUpdate() {
        val update = _updateAvailable.value ?: return
        if (_downloadState.value.isDownloading) return
        UpdateNotificationManager.cancelUpdateNotification(context)

        val cachedUri = updateDownloader.getDownloadedApkUri()
        if (cachedUri != null) {
            _downloadState.value = DownloadState(isComplete = true, uri = cachedUri, progress = 1f)
            launchInstall(cachedUri)
            return
        }

        viewModelScope.launch {
            updateDownloader.download(update.downloadUrl)
        }
    }

    private fun launchInstall(uri: android.net.Uri) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) { }
    }

    fun installUpdate() {
        val uri = updateDownloader.getDownloadedApkUri()
        if (uri != null) {
            _downloadState.value = DownloadState(isComplete = true, uri = uri, progress = 1f)
            launchInstall(uri)
        }
    }

    fun cancelDownload() {
        updateDownloader.cancel()
        _downloadState.value = DownloadState()
    }

    fun dismissUpdate() {
        _updateDismissed.value = true
        UpdateNotificationManager.cancelUpdateNotification(context)
    }

    private fun collectDownloadState() {
        viewModelScope.launch {
            updateDownloader.state.collect { state ->
                _downloadState.value = state
                if (state.isComplete && state.uri != null) {
                    launchInstall(state.uri)
                }
            }
        }
    }

    private fun startUserRoleListener(shopCode: String, userId: String) {
        _userRoleListenerJob?.cancel()
        if (shopCode.isBlank() || userId.isBlank()) return
        _userRoleListenerJob = viewModelScope.launch {
            firebaseClient.subscribeToUserRole(shopCode, userId).collect { role ->
                if (role != null && role != _userRole.value) {
                    _userRole.value = role
                    _isOwner.value = role == "owner"
                    context.dataStore.edit { prefs ->
                        prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ROLE)] = role
                    }
                    loadMembers()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        _userRoleListenerJob?.cancel()
    }

    private fun generateQrBitmap() {
        try {
            val raw = "BILLING:${_shopCode.value}:${_shopSecret.value}"
            val content = java.net.URLEncoder.encode(raw, "UTF-8")
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            _qrBitmap.value = bitmap
        } catch (e: Exception) {
            Log.e("SettingsVM", "QR generation failed", e)
        }
    }
}

sealed class BackupState {
    data object Idle : BackupState()
    data object InProgress : BackupState()
    data class Success(val fileName: String) : BackupState()
    data class Error(val message: String) : BackupState()
}

sealed class RestoreState {
    data object Idle : RestoreState()
    data object InProgress : RestoreState()
    data class Success(val summary: String) : RestoreState()
    data class Error(val message: String) : RestoreState()
}
