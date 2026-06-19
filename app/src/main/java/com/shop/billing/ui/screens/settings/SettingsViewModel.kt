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
import com.shop.billing.data.local.dao.ProductDao
import com.shop.billing.data.local.entity.InvoiceEntity
import com.shop.billing.data.local.entity.InvoiceItemEntity
import com.shop.billing.data.local.entity.ProductEntity

import com.shop.billing.data.remote.AppVersion
import com.shop.billing.data.remote.DownloadState
import com.shop.billing.data.remote.FirebaseClient
import com.shop.billing.data.remote.UpdateDownloader
import com.shop.billing.data.remote.UpdateManager
import com.shop.billing.data.sync.LogEntry
import com.shop.billing.data.sync.LogType
import com.shop.billing.data.sync.SyncEngine
import com.shop.billing.data.sync.SyncService
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
import kotlinx.coroutines.flow.first
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

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncEngine: SyncEngine,
    private val firebaseClient: FirebaseClient,
    private val productDao: ProductDao,
    private val customerDao: CustomerDao,
    private val invoiceDao: InvoiceDao,
    private val invoiceItemDao: InvoiceItemDao,
    private val customerPaymentDao: CustomerPaymentDao
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

    private val _dbStats = MutableStateFlow<Map<String, Int>>(emptyMap())
    val dbStats: StateFlow<Map<String, Int>> = _dbStats

    private val _qrBitmap = MutableStateFlow<Bitmap?>(null)
    val qrBitmap: StateFlow<Bitmap?> = _qrBitmap

    private val _configQrBitmap = MutableStateFlow<Bitmap?>(null)
    val configQrBitmap: StateFlow<Bitmap?> = _configQrBitmap

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

    private val _currentUserId = MutableStateFlow("")
    val currentUserId: StateFlow<String> = _currentUserId

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
            _userRole.value = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ROLE)] ?: "member"
            _isOwner.value = _userRole.value == "owner"
            _currentUserId.value = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ID)] ?: ""

            if (localShopCode.isNotBlank()) {
                if (_shopSecret.value.isNotBlank()) {
                    generateQrBitmap()
                }
                loadMembers()
                startUserRoleListener(localShopCode, _currentUserId.value)
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
                        val bitmap = decodeImage(bytes, uri)
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

    private fun decodeImage(bytes: ByteArray, uri: Uri): Bitmap? {
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
                                put("price", item.price)
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
                                price = itemObj.getDouble("price"),
                                category = itemObj.optString("category", "General"),
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

    fun loadDbStats() {
        viewModelScope.launch {
            val code = _shopCode.value
            if (code.isBlank()) return@launch
            try {
                val products = productDao.getAll(code).size
                val customers = customerDao.getAll(code).size
                val invoices = invoiceDao.getAll(code).size
                _dbStats.value = mapOf(
                    "products" to products,
                    "customers" to customers,
                    "invoices" to invoices
                )
            } catch (e: Exception) {
                Log.e("SettingsVM", "loadDbStats failed", e)
            }
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

    fun checkForUpdates() {
        if (_isCheckingUpdate.value) return
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            val result = withContext(Dispatchers.IO) {
                updateManager.checkForUpdate()
            }
            _updateAvailable.value = result
            _isCheckingUpdate.value = false
        }
    }

    fun downloadUpdate() {
        val update = _updateAvailable.value ?: return
        if (_downloadState.value.isDownloading) return
        viewModelScope.launch {
            updateDownloader.download(update.downloadUrl)
        }
    }

    fun cancelDownload() {
        updateDownloader.cancel()
        _downloadState.value = DownloadState()
    }

    fun dismissUpdate() {
        _updateAvailable.value = null
    }

    private fun collectDownloadState() {
        viewModelScope.launch {
            updateDownloader.state.collect { state ->
                _downloadState.value = state
                if (state.isComplete && state.uri != null) {
                    try {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                            setDataAndType(state.uri, "application/vnd.android.package-archive")
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) { }
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
