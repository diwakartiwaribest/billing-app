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
import com.shop.billing.data.model.Bill
import com.shop.billing.data.model.BillItem
import com.shop.billing.data.model.ShopItem
import com.shop.billing.data.AppDataCache
import com.shop.billing.data.remote.SupabaseClient
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val supabaseClient: SupabaseClient,
    private val dataCache: AppDataCache
) : ViewModel() {

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

    private val _syncEnabled = MutableStateFlow(false)
    val syncEnabled: StateFlow<Boolean> = _syncEnabled

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus

    private val _dbStats = MutableStateFlow<Map<String, Int>>(emptyMap())
    val dbStats: StateFlow<Map<String, Int>> = _dbStats

    private val _dbDetails = MutableStateFlow<Map<String, Any>>(emptyMap())
    val dbDetails: StateFlow<Map<String, Any>> = _dbDetails

    private val _qrBitmap = MutableStateFlow<Bitmap?>(null)
    val qrBitmap: StateFlow<Bitmap?> = _qrBitmap

    private val _configQrBitmap = MutableStateFlow<Bitmap?>(null)
    val configQrBitmap: StateFlow<Bitmap?> = _configQrBitmap

    private val _joinStatus = MutableStateFlow<JoinStatus>(JoinStatus.Idle)
    val joinStatus: StateFlow<JoinStatus> = _joinStatus

    private val _supabaseUrl = MutableStateFlow("")
    val supabaseUrl: StateFlow<String> = _supabaseUrl

    private val _supabaseKey = MutableStateFlow("")
    val supabaseKey: StateFlow<String> = _supabaseKey

    private val _invoiceMessage = MutableStateFlow(Constants.DEFAULT_INVOICE_MESSAGE)
    val invoiceMessage: StateFlow<String> = _invoiceMessage

    private val _projectRef = MutableStateFlow("")
    val projectRef: StateFlow<String> = _projectRef

    private val _personalAccessToken = MutableStateFlow("")
    val personalAccessToken: StateFlow<String> = _personalAccessToken

    private val _userRole = MutableStateFlow("member")
    val userRole: StateFlow<String> = _userRole

    private val _userId = MutableStateFlow("")
    val userId: StateFlow<String> = _userId

    private val _userEmail = MutableStateFlow("")
    val userEmail: StateFlow<String> = _userEmail

    data class ShopMember(val userId: String, val role: String, val deviceName: String, val email: String = "")

    private val _members = MutableStateFlow<List<ShopMember>>(emptyList())
    val members: StateFlow<List<ShopMember>> = _members

    init {
        PdfGenerator.ensureTemplateFilesExist(context)
        _templatePath.value = PdfGenerator.getTemplatesDir(context).absolutePath

        // Load DB stats from cache instantly (before first frame)
        if (dataCache.dbStatsLoaded) {
            _dbStats.value = dataCache.dbStats
            _dbDetails.value = dataCache.dbDetails
        }

        viewModelScope.launch {
            val prefs = context.dataStore.data.first()
            _shopName.value = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_NAME)] ?: Constants.DEFAULT_SHOP_NAME
            _shopAddress.value = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_ADDRESS)] ?: Constants.DEFAULT_SHOP_ADDRESS
            _shopPhone.value = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_PHONE)] ?: Constants.DEFAULT_SHOP_PHONE
            _logoUri.value = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_LOGO)]
            _shopCode.value = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: ""
            _shopSecret.value = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_SECRET)] ?: ""
            _syncEnabled.value = prefs[booleanPreferencesKey(Constants.SETTINGS_KEY_SYNC_ENABLED)] ?: false
            _supabaseUrl.value = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_URL)] ?: ""
            _supabaseKey.value = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_KEY)] ?: ""
            _invoiceMessage.value = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_INVOICE_MESSAGE)] ?: Constants.DEFAULT_INVOICE_MESSAGE
            _projectRef.value = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_PROJECT_REF)] ?: ""
            _personalAccessToken.value = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_PAT)] ?: ""
            _userRole.value = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ROLE)] ?: "member"
            _userId.value = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ID)] ?: ""
            _userEmail.value = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_EMAIL)] ?: ""

            // Auto-restore config from Supabase if shop code is set in DataStore
            if (_shopCode.value.isNotBlank()) {
                loadConfigFromSupabase(_shopCode.value)
                // Push local DataStore config to Supabase (creates table/columns if needed)
                withContext(Dispatchers.IO) { saveConfigToSupabase() }
                // Reload after push to pick up any newly created data
                loadConfigFromSupabase(_shopCode.value)
            }

            if (_syncEnabled.value && _shopCode.value.isNotBlank()) {
                startSync(includeLogo = true)
                startAutoSync()
            }

            val pat = _personalAccessToken.value
            val ref = _projectRef.value
            if (pat.isNotBlank() && ref.isNotBlank()) {
                withContext(Dispatchers.IO) {
                    supabaseClient.enableRealtimePublication(pat, ref)
                }
            }

            if (_shopCode.value.isNotBlank() && _shopSecret.value.isNotBlank()) {
                generateQrBitmap()
            }
        }
    }

    fun updateShopName(name: String) {
        _shopName.value = name
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_NAME)] = name
            }
            saveConfigToSupabase()
        }
    }

    fun updateShopAddress(address: String) {
        _shopAddress.value = address
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_ADDRESS)] = address
            }
        }
    }

    fun updateShopPhone(phone: String) {
        _shopPhone.value = phone
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_PHONE)] = phone
            }
        }
    }

    fun updateInvoiceMessage(message: String) {
        _invoiceMessage.value = message
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[stringPreferencesKey(Constants.SETTINGS_KEY_INVOICE_MESSAGE)] = message
            }
        }
    }

    fun updateProjectRef(ref: String) {
        _projectRef.value = ref
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[stringPreferencesKey(Constants.SETTINGS_KEY_PROJECT_REF)] = ref
            }
            saveConfigToSupabase()
        }
    }

    fun updatePersonalAccessToken(token: String) {
        _personalAccessToken.value = token
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[stringPreferencesKey(Constants.SETTINGS_KEY_PAT)] = token
            }
            saveConfigToSupabase()
        }
    }

    fun updateSupabaseUrl(url: String) {
        _supabaseUrl.value = url
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_URL)] = url
            }
            saveConfigToSupabase()
        }
    }

    fun updateSupabaseKey(key: String) {
        _supabaseKey.value = key
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_KEY)] = key
            }
            saveConfigToSupabase()
        }
    }

    fun saveLogo(uri: Uri) {
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
        _logoUri.value = null
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs.remove(stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_LOGO))
            }
        }
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

                    val url = _supabaseUrl.value
                    val key = _supabaseKey.value
                    val code = _shopCode.value

                    val root = JSONObject()

                    if (backupBills.value && url.isNotBlank() && key.isNotBlank() && code.isNotBlank()) {
                        val (bills, billItems) = supabaseClient.pullBills(url, key, code)
                        val billsArray = JSONArray()
                        val grouped = billItems.groupBy { it.billId }
                        for (bill in bills) {
                            val items = grouped[bill.id] ?: emptyList()
                            val itemsArray = JSONArray()
                            for (item in items) {
                                itemsArray.put(JSONObject().apply {
                                    put("id", item.id)
                                    put("billId", item.billId)
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
                                put("createdAt", bill.createdAt)
                                put("createdBy", bill.createdBy)
                                put("items", itemsArray)
                            })
                        }
                        root.put("bills", billsArray)
                    }

                    if (backupShopItems.value && url.isNotBlank() && key.isNotBlank() && code.isNotBlank()) {
                        val shopItems = supabaseClient.pullShopItems(url, key, code)
                        val itemsArray = JSONArray()
                        for (item in shopItems) {
                            itemsArray.put(JSONObject().apply {
                                put("id", item.id)
                                put("name", item.name)
                                put("price", item.price)
                                put("category", item.category)
                                put("createdAt", item.createdAt)
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
                            put(Constants.SETTINGS_KEY_SUPABASE_URL, prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_URL)] ?: "")
                            put(Constants.SETTINGS_KEY_SUPABASE_KEY, prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_KEY)] ?: "")
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

                    val url = _supabaseUrl.value
                    val key = _supabaseKey.value
                    val code = _shopCode.value

                    if (restoreBills.value && root.has("bills") && url.isNotBlank() && key.isNotBlank() && code.isNotBlank()) {
                        val billsArray = root.getJSONArray("bills")
                        for (i in 0 until billsArray.length()) {
                            val billObj = billsArray.getJSONObject(i)
                            val bill = Bill(
                                id = billObj.getString("id"),
                                billNumber = billObj.getString("billNumber"),
                                customerName = billObj.optString("customerName", ""),
                                customerMobile = billObj.optString("customerMobile", ""),
                                totalAmount = billObj.getDouble("totalAmount"),
                                createdAt = billObj.getLong("createdAt"),
                                createdBy = billObj.optString("createdBy", "")
                            )
                            val items = if (billObj.has("items")) {
                                val itemsArray = billObj.getJSONArray("items")
                                (0 until itemsArray.length()).map { j ->
                                    val itemObj = itemsArray.getJSONObject(j)
                                    BillItem(
                                        id = itemObj.getString("id"),
                                        billId = itemObj.getString("billId"),
                                        itemName = itemObj.getString("itemName"),
                                        quantity = itemObj.getInt("quantity"),
                                        unitPrice = itemObj.getDouble("unitPrice"),
                                        subtotal = itemObj.getDouble("subtotal")
                                    )
                                }
                            } else emptyList()
                            supabaseClient.pushBill(url, key, code, bill, items)
                            billsRestored++
                            itemsRestored += items.size
                        }
                    }

                    if (restoreShopItems.value && root.has("shop_items") && url.isNotBlank() && key.isNotBlank() && code.isNotBlank()) {
                        val itemsArray = root.getJSONArray("shop_items")
                        for (i in 0 until itemsArray.length()) {
                            val itemObj = itemsArray.getJSONObject(i)
                            val item = ShopItem(
                                id = itemObj.getString("id"),
                                name = itemObj.getString("name"),
                                price = itemObj.getDouble("price"),
                                category = itemObj.getString("category"),
                                createdAt = itemObj.getLong("createdAt")
                            )
                            supabaseClient.pushShopItem(url, key, code, item)
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
                            if (settingsObj.has(Constants.SETTINGS_KEY_SUPABASE_URL)) {
                                prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_URL)] = settingsObj.getString(Constants.SETTINGS_KEY_SUPABASE_URL)
                            }
                            if (settingsObj.has(Constants.SETTINGS_KEY_SUPABASE_KEY)) {
                                prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_KEY)] = settingsObj.getString(Constants.SETTINGS_KEY_SUPABASE_KEY)
                            }
                            if (settingsObj.has(Constants.SETTINGS_KEY_SHOP_CODE)) {
                                prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] = settingsObj.getString(Constants.SETTINGS_KEY_SHOP_CODE)
                            }
                            if (settingsObj.has(Constants.SETTINGS_KEY_SHOP_SECRET)) {
                                prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_SECRET)] = settingsObj.getString(Constants.SETTINGS_KEY_SHOP_SECRET)
                            }
                        }
                    }

                    if (restoreSettings.value && root.has("settings")) {
                        val s = root.getJSONObject("settings")
                        if (s.has(Constants.SETTINGS_KEY_SUPABASE_URL)) _supabaseUrl.value = s.getString(Constants.SETTINGS_KEY_SUPABASE_URL)
                        if (s.has(Constants.SETTINGS_KEY_SUPABASE_KEY)) _supabaseKey.value = s.getString(Constants.SETTINGS_KEY_SUPABASE_KEY)
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

                    if (_syncEnabled.value && _shopCode.value.isNotBlank()) {
                        try {
                            pushAllDataToSupabase(includeLogo = true)
                        } catch (e: Exception) {
                            Log.e("SettingsVM", "Post-restore sync failed", e)
                        }
                    }
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
            if (code.isNotBlank()) loadConfigFromSupabase(code)
        }
    }

    fun updateShopSecret(secret: String) {
        _shopSecret.value = secret
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_SECRET)] = secret
            }
            saveConfigToSupabase()
            if (_shopCode.value.isNotBlank() && secret.isNotBlank()) {
                generateQrBitmap()
            }
        }
    }

    private suspend fun saveConfigToSupabase() {
        val code = _shopCode.value
        if (code.isBlank()) return
        withContext(Dispatchers.IO) {
            // Merge local values with existing remote values so we don't overwrite remote with blanks
            val remote = supabaseClient.loadShopConfig(code)
            val pat = _personalAccessToken.value.ifBlank { remote?.optString("pat", "") ?: "" }
            val ref = _projectRef.value.ifBlank { remote?.optString("project_ref", "") ?: "" }
            val secret = _shopSecret.value.ifBlank { remote?.optString("secret", "") ?: "" }
            val url = _supabaseUrl.value.ifBlank { remote?.optString("supabase_url", "") ?: "" }
            val key = _supabaseKey.value.ifBlank { remote?.optString("supabase_key", "") ?: "" }
            val name = _shopName.value.ifBlank { remote?.optString("shop_name", "") ?: "" }
            val sync = _syncEnabled.value || remote?.optBoolean("sync_enabled", false) == true

            // If columns don't exist (remote == null) and we have management API access, create them first
            if (remote == null && pat.isNotBlank() && ref.isNotBlank()) {
                supabaseClient.createTablesViaManagementApi(pat, ref)
            }

            var saved = supabaseClient.saveShopConfig(
                code = code,
                supabaseUrl = url,
                supabaseKey = key,
                projectRef = ref,
                pat = pat,
                shopSecret = secret,
                shopName = name,
                syncEnabled = sync
            )
            if (!saved && pat.isNotBlank() && ref.isNotBlank()) {
                supabaseClient.createTablesViaManagementApi(pat, ref)
                saved = supabaseClient.saveShopConfig(
                    code = code,
                    supabaseUrl = url,
                    supabaseKey = key,
                    projectRef = ref,
                    pat = pat,
                    shopSecret = secret,
                    shopName = name,
                    syncEnabled = sync
                )
            }

            // Enable Realtime publication if PAT/ref are available
            val finalPat = pat.ifBlank { _personalAccessToken.value }
            val finalRef = ref.ifBlank { _projectRef.value }
            if (finalPat.isNotBlank() && finalRef.isNotBlank()) {
                supabaseClient.enableRealtimePublication(finalPat, finalRef)
            }
        }
    }

    private suspend fun loadConfigFromSupabase(code: String) {
        val config = withContext(Dispatchers.IO) {
            supabaseClient.loadShopConfig(code)
        } ?: return

        val remoteUrl = config.optString("supabase_url", "")
        val remoteKey = config.optString("supabase_key", "")
        val remoteRef = config.optString("project_ref", "")
        val remotePat = config.optString("pat", "")
        val remoteSecret = config.optString("secret", "")
        val remoteName = config.optString("shop_name", "")
        val remoteSync = config.optBoolean("sync_enabled", false)

        context.dataStore.edit { prefs ->
            if (remoteUrl.isNotBlank() && _supabaseUrl.value.isBlank()) {
                prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_URL)] = remoteUrl
                _supabaseUrl.value = remoteUrl
            }
            if (remoteKey.isNotBlank() && _supabaseKey.value.isBlank()) {
                prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_KEY)] = remoteKey
                _supabaseKey.value = remoteKey
            }
            if (remoteRef.isNotBlank() && _projectRef.value.isBlank()) {
                prefs[stringPreferencesKey(Constants.SETTINGS_KEY_PROJECT_REF)] = remoteRef
                _projectRef.value = remoteRef
            }
            if (remotePat.isNotBlank() && _personalAccessToken.value.isBlank()) {
                prefs[stringPreferencesKey(Constants.SETTINGS_KEY_PAT)] = remotePat
                _personalAccessToken.value = remotePat
            }
            if (remoteSecret.isNotBlank() && _shopSecret.value.isBlank()) {
                prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_SECRET)] = remoteSecret
                _shopSecret.value = remoteSecret
            }
            if (remoteName.isNotBlank() && _shopName.value.isBlank()) {
                prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_NAME)] = remoteName
                _shopName.value = remoteName
            }
            if (remoteSync && !_syncEnabled.value) {
                prefs[booleanPreferencesKey(Constants.SETTINGS_KEY_SYNC_ENABLED)] = true
                _syncEnabled.value = true
                startSync(includeLogo = true)
                startAutoSync()
            }
        }

        val finalPat = _personalAccessToken.value
        val finalRef = _projectRef.value
        if (finalPat.isNotBlank() && finalRef.isNotBlank()) {
            withContext(Dispatchers.IO) {
                supabaseClient.enableRealtimePublication(finalPat, finalRef)
            }
        }
        if (_shopSecret.value.isNotBlank()) generateQrBitmap()
    }

    private var autoSyncJob: Job? = null

    fun toggleSync() {
        val newState = !_syncEnabled.value
        _syncEnabled.value = newState
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[booleanPreferencesKey(Constants.SETTINGS_KEY_SYNC_ENABLED)] = newState
            }
            saveConfigToSupabase()
            if (newState && _shopCode.value.isNotBlank()) {
                startSync(includeLogo = true)
                startAutoSync()
            } else {
                autoSyncJob?.cancel()
                autoSyncJob = null
                _syncStatus.value = SyncStatus.Idle
            }
        }
    }

    private fun startAutoSync() {
        autoSyncJob?.cancel()
        autoSyncJob = viewModelScope.launch {
            while (isActive) {
                delay(15 * 60 * 1000L)
                if (_syncEnabled.value && _shopCode.value.isNotBlank() && _supabaseUrl.value.isNotBlank()) {
                    withContext(Dispatchers.IO) {
                        try {
                            pullAllDataFromSupabase(includeLogo = false)
                            pushAllDataToSupabase(includeLogo = false)
                        } catch (e: Exception) {
                            Log.e("SettingsVM", "Auto-sync failed", e)
                        }
                    }
                }
            }
        }
    }

    private fun startSync(includeLogo: Boolean = true) {
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.Syncing
            withContext(Dispatchers.IO) {
                try {
                    val url = _supabaseUrl.value
                    val key = _supabaseKey.value
                    if (url.isBlank() || key.isBlank()) {
                        _syncStatus.value = SyncStatus.Error("Configure Supabase URL and API key first")
                        return@withContext
                    }
                    pullAllDataFromSupabase(includeLogo)
                    pushAllDataToSupabase(includeLogo)
                    _syncStatus.value = SyncStatus.Connected
                } catch (e: Exception) {
                    Log.e("SettingsVM", "Sync failed", e)
                    _syncStatus.value = SyncStatus.Error(e.message ?: "Sync failed")
                }
            }
        }
    }

    fun manualSync() {
        if (_shopCode.value.isBlank()) {
            _syncStatus.value = SyncStatus.Error("Enter a shop code first")
            return
        }
        startSync(includeLogo = true)
    }

    private suspend fun pushAllDataToSupabase(includeLogo: Boolean = true) {
        val url = _supabaseUrl.value
        val key = _supabaseKey.value
        val code = _shopCode.value
        val secret = _shopSecret.value

        val pat = _personalAccessToken.value
        val ref = _projectRef.value
        if (pat.isNotBlank() && ref.isNotBlank()) {
            try {
                supabaseClient.createTablesViaManagementApi(pat, ref)
            } catch (e: Exception) {
                Log.e("SettingsVM", "Auto-create tables failed", e)
            }
        }

        try {
            supabaseClient.ensureShopExists(url, key, code, secret)
        } catch (e: Exception) {
            Log.e("SettingsVM", "ensureShopExists failed", e)
        }

        if (_userRole.value == "owner") {
            try {
                supabaseClient.pushSettings(url, key, code, _shopName.value, _shopAddress.value, _shopPhone.value, _logoUri.value ?: "", includeLogo, _invoiceMessage.value)
            } catch (e: Exception) {
                Log.e("SettingsVM", "Push settings failed", e)
            }
        }
    }

    private suspend fun pullAllDataFromSupabase(includeLogo: Boolean = true) {
        val url = _supabaseUrl.value
        val key = _supabaseKey.value
        val code = _shopCode.value

        val settings = supabaseClient.pullSettings(url, key, code, includeLogo)
        if (settings != null) {
            Log.d("SettingsVM", "pullSettings success: shop_name=${settings.optString("shop_name")}")
            context.dataStore.edit { prefs ->
                if (settings.has("shop_name")) {
                    val v = settings.getString("shop_name")
                    prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_NAME)] = v
                    _shopName.value = v
                }
                if (settings.has("shop_address")) {
                    val v = settings.getString("shop_address")
                    prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_ADDRESS)] = v
                    _shopAddress.value = v
                }
                if (settings.has("shop_phone")) {
                    val v = settings.getString("shop_phone")
                    prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_PHONE)] = v
                    _shopPhone.value = v
                }
                if (includeLogo && settings.has("shop_logo")) {
                    val v = settings.getString("shop_logo")
                    prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_LOGO)] = v
                    _logoUri.value = v
                }
                if (settings.has("invoice_message")) {
                    val v = settings.getString("invoice_message") ?: ""
                    prefs[stringPreferencesKey(Constants.SETTINGS_KEY_INVOICE_MESSAGE)] = v
                    _invoiceMessage.value = v
                }
            }
        } else {
            Log.w("SettingsVM", "pullSettings returned null for shop_code=$code")
        }

        val uid = _userId.value
        if (uid.isNotBlank()) {
            try {
                val role = supabaseClient.getUserRole(url, key, code, uid)
                if (role == "member" && !supabaseClient.isOwnerInSupabase(url, key, code)) {
                    supabaseClient.claimOwnershipIfNoOwner(url, key, code, uid)
                    context.dataStore.edit { prefs ->
                        prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ROLE)] = "owner"
                    }
                    _userRole.value = "owner"
                } else {
                    context.dataStore.edit { prefs ->
                        prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ROLE)] = role
                    }
                    _userRole.value = role
                }
            } catch (e: Exception) {
                Log.e("SettingsVM", "Failed to pull user role", e)
            }
        }
    }

    fun createNewShop(code: String) {
        if (code.isBlank()) {
            _syncStatus.value = SyncStatus.Error("Shop code cannot be empty")
            return
        }
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.Syncing
            withContext(Dispatchers.IO) {
                try {
                    val url = _supabaseUrl.value
                    val key = _supabaseKey.value
                    if (url.isBlank() || key.isBlank()) {
                        _syncStatus.value = SyncStatus.Error("Configure Supabase URL and API key first")
                        return@withContext
                    }

                    val pat = _personalAccessToken.value
                    val ref = _projectRef.value
                    if (pat.isNotBlank() && ref.isNotBlank()) {
                        supabaseClient.createTablesViaManagementApi(pat, ref)
                    }

                    val secret = supabaseClient.generateSecret()
                    supabaseClient.createShop(url, key, code, secret)

                    val uid = _userId.value
                    if (uid.isNotBlank()) {
                        supabaseClient.registerUserShop(url, key, uid, code, "owner", email = _userEmail.value)
                    }
                    context.dataStore.edit { prefs ->
                        prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ROLE)] = "owner"
                    }
                    _userRole.value = "owner"

                    _shopCode.value = code
                    _shopSecret.value = secret
                    context.dataStore.edit { prefs ->
                        prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] = code
                        prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_SECRET)] = secret
                        prefs[booleanPreferencesKey(Constants.SETTINGS_KEY_SYNC_ENABLED)] = true
                    }
                    saveConfigToSupabase()
                    generateQrBitmap()
                    _syncStatus.value = SyncStatus.Connected
                } catch (e: Exception) {
                    Log.e("SettingsVM", "Create shop failed", e)
                    _syncStatus.value = SyncStatus.Error(e.message ?: "Failed to create shop")
                }
            }
        }
    }

    fun joinShop(code: String, secret: String, urlOverride: String = "", keyOverride: String = "", patOverride: String = "", projectRefOverride: String = "") {
        if (code.isBlank() || secret.isBlank()) {
            _joinStatus.value = JoinStatus.Error("Code and secret are required")
            return
        }
        viewModelScope.launch {
            _joinStatus.value = JoinStatus.Loading
            withContext(Dispatchers.IO) {
                try {
                    val url = if (urlOverride.isNotBlank()) urlOverride else _supabaseUrl.value
                    val key = if (keyOverride.isNotBlank()) keyOverride else _supabaseKey.value
                    if (url.isBlank() || key.isBlank()) {
                        _joinStatus.value = JoinStatus.Error("Configure Supabase URL and API key first")
                        return@withContext
                    }

                    if (urlOverride.isNotBlank()) {
                        _supabaseUrl.value = urlOverride
                        context.dataStore.edit { prefs ->
                            prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_URL)] = urlOverride
                        }
                    }
                    if (keyOverride.isNotBlank()) {
                        _supabaseKey.value = keyOverride
                        context.dataStore.edit { prefs ->
                            prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_KEY)] = keyOverride
                        }
                    }
                    if (patOverride.isNotBlank()) {
                        _personalAccessToken.value = patOverride
                        context.dataStore.edit { prefs ->
                            prefs[stringPreferencesKey(Constants.SETTINGS_KEY_PAT)] = patOverride
                        }
                    }
                    if (projectRefOverride.isNotBlank()) {
                        _projectRef.value = projectRefOverride
                        context.dataStore.edit { prefs ->
                            prefs[stringPreferencesKey(Constants.SETTINGS_KEY_PROJECT_REF)] = projectRefOverride
                        }
                    }

                    val pat = _personalAccessToken.value
                    val ref = _projectRef.value
                    if (pat.isNotBlank() && ref.isNotBlank()) {
                        supabaseClient.createTablesViaManagementApi(pat, ref)
                    }

                    val valid = supabaseClient.validateSecret(url, key, code, secret)
                    if (valid) {
                        val uid = _userId.value
                        if (uid.isNotBlank()) {
                            supabaseClient.registerUserShop(url, key, uid, code, "member", email = _userEmail.value)
                        }
                        context.dataStore.edit { prefs ->
                            prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ROLE)] = "member"
                        }
                        _userRole.value = "member"

                        _shopCode.value = code
                        _shopSecret.value = secret
                        _supabaseUrl.value = url
                        _supabaseKey.value = key
                        context.dataStore.edit { prefs ->
                            prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] = code
                            prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_SECRET)] = secret
                            prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_URL)] = url
                            prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_KEY)] = key
                            prefs[booleanPreferencesKey(Constants.SETTINGS_KEY_SYNC_ENABLED)] = true
                        }
                        _syncEnabled.value = true
                        saveConfigToSupabase()
                        generateQrBitmap()
                        _joinStatus.value = JoinStatus.Success
                        startSync(includeLogo = true)
                        startAutoSync()
                    } else {
                        _joinStatus.value = JoinStatus.Error("Invalid code or secret")
                    }
                } catch (e: Exception) {
                    Log.e("SettingsVM", "Join shop failed", e)
                    _joinStatus.value = JoinStatus.Error(e.message ?: "Failed to join shop")
                }
            }
        }
    }

    fun isOwner(): Boolean = _userRole.value == "owner"

    fun startMembersAutoRefresh() {
        viewModelScope.launch {
            while (isActive) {
                delay(30 * 1000L)
                if (_shopCode.value.isNotBlank() && _supabaseUrl.value.isNotBlank()) {
                    pullMembers()
                }
            }
        }
    }

    fun pullMembers() {
        val url = _supabaseUrl.value
        val key = _supabaseKey.value
        val code = _shopCode.value
        if (url.isBlank() || key.isBlank() || code.isBlank()) return

        viewModelScope.launch {
            try {
                val arr = withContext(Dispatchers.IO) {
                    supabaseClient.pullUserShops(url, key, code)
                }
                val list = mutableListOf<ShopMember>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(ShopMember(
                        userId = obj.getString("user_id"),
                        role = obj.getString("role"),
                        deviceName = if (obj.has("device_name")) obj.getString("device_name") else "",
                        email = if (obj.has("email")) obj.getString("email") else ""
                    ))
                }
                _members.value = list
                val currentEmail = _userEmail.value
                val myRecord = list.find { it.userId == _userId.value }
                if (myRecord != null) {
                    if (myRecord.role.isNotBlank() && myRecord.role != _userRole.value) {
                        _userRole.value = myRecord.role
                        context.dataStore.edit { prefs ->
                            prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ROLE)] = myRecord.role
                        }
                    }
                    if (currentEmail.isNotBlank() && myRecord.email != currentEmail) {
                        withContext(Dispatchers.IO) {
                            supabaseClient.updateUserShopField(url, key, _userId.value, code, "email", currentEmail)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("SettingsVM", "pullMembers failed", e)
            }
        }
    }

    fun transferOwnership(targetUserId: String) {
        val url = _supabaseUrl.value
        val key = _supabaseKey.value
        val code = _shopCode.value
        val currentUserId = _userId.value
        if (url.isBlank() || key.isBlank() || code.isBlank() || currentUserId.isBlank()) return

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    supabaseClient.transferOwnership(url, key, code, targetUserId, currentUserId)
                }
                context.dataStore.edit { prefs ->
                    prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ROLE)] = "member"
                }
                _userRole.value = "member"
                pullMembers()
            } catch (e: Exception) {
                Log.e("SettingsVM", "transferOwnership failed", e)
            }
        }
    }

    fun removeMember(targetUserId: String) {
        val url = _supabaseUrl.value
        val key = _supabaseKey.value
        val code = _shopCode.value
        if (url.isBlank() || key.isBlank() || code.isBlank()) return

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    supabaseClient.removeMember(url, key, code, targetUserId)
                }
                pullMembers()
            } catch (e: Exception) {
                Log.e("SettingsVM", "removeMember failed", e)
            }
        }
    }

    fun leaveShop() {
        val url = _supabaseUrl.value
        val key = _supabaseKey.value
        val code = _shopCode.value
        val uid = _userId.value
        if (url.isBlank() || key.isBlank() || code.isBlank() || uid.isBlank()) return

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    supabaseClient.leaveShop(url, key, code, uid)
                }
                context.dataStore.edit { prefs ->
                    prefs.remove(stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE))
                    prefs.remove(stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_SECRET))
                    prefs.remove(stringPreferencesKey(Constants.SETTINGS_KEY_USER_ROLE))
                    prefs[booleanPreferencesKey(Constants.SETTINGS_KEY_SYNC_ENABLED)] = false
                }
                _shopCode.value = ""
                _shopSecret.value = ""
                _userRole.value = "member"
                _syncEnabled.value = false
                _members.value = emptyList()
            } catch (e: Exception) {
                Log.e("SettingsVM", "leaveShop failed", e)
            }
        }
    }

    fun deleteShop() {
        val url = _supabaseUrl.value
        val key = _supabaseKey.value
        val code = _shopCode.value
        if (url.isBlank() || key.isBlank() || code.isBlank()) return

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    supabaseClient.deleteShop(url, key, code)
                }
                context.dataStore.edit { prefs ->
                    prefs.remove(stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE))
                    prefs.remove(stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_SECRET))
                    prefs.remove(stringPreferencesKey(Constants.SETTINGS_KEY_USER_ROLE))
                    prefs[booleanPreferencesKey(Constants.SETTINGS_KEY_SYNC_ENABLED)] = false
                }
                _shopCode.value = ""
                _shopSecret.value = ""
                _userRole.value = "member"
                _syncEnabled.value = false
                _members.value = emptyList()
            } catch (e: Exception) {
                Log.e("SettingsVM", "deleteShop failed", e)
            }
        }
    }

    fun loadDbStats() {
        val url = _supabaseUrl.value
        val key = _supabaseKey.value
        val code = _shopCode.value
        val pat = _personalAccessToken.value
        val ref = _projectRef.value
        if (url.isBlank() || key.isBlank() || code.isBlank()) return

        // Use cached data instantly if available
        if (dataCache.dbStatsLoaded) {
            _dbStats.value = dataCache.dbStats
            _dbDetails.value = dataCache.dbDetails
        }

        viewModelScope.launch {
            try {
                val stats = withContext(Dispatchers.IO) {
                    supabaseClient.getAllRowCounts(url, key, code)
                }
                _dbStats.value = stats

                if (pat.isNotBlank() && ref.isNotBlank()) {
                    val details = withContext(Dispatchers.IO) {
                        supabaseClient.getDatabaseStats(pat, ref)
                    }
                    _dbDetails.value = details
                }

                // Update cache
                dataCache.setDbStats(_dbStats.value, _dbDetails.value)
            } catch (e: Exception) {
                Log.e("SettingsVM", "loadDbStats failed", e)
            }
        }
    }

    suspend fun createTablesAutomatically(pat: String, ref: String): Boolean {
        return try {
            supabaseClient.createTablesViaManagementApi(pat, ref)
        } catch (e: Exception) {
            Log.e("SettingsVM", "createTablesAutomatically failed", e)
            false
        }
    }

    fun getTableCreationSql(): String {
        return supabaseClient.getTableCreationSql()
    }

    fun resetJoinStatus() {
        _joinStatus.value = JoinStatus.Idle
    }

    private fun generateQrBitmap() {
        try {
            val pat = _personalAccessToken.value
            val ref = _projectRef.value
            val raw = if (pat.isNotBlank() && ref.isNotBlank()) {
                "BILLING_SYNC:${_shopCode.value}:${_shopSecret.value}:${_supabaseUrl.value}:${_supabaseKey.value}:$pat:$ref"
            } else {
                "BILLING_SYNC:${_shopCode.value}:${_shopSecret.value}:${_supabaseUrl.value}:${_supabaseKey.value}"
            }
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

    fun generateConfigQrBitmap() {
        try {
            val pat = _personalAccessToken.value
            val ref = _projectRef.value
            val raw = if (pat.isNotBlank() && ref.isNotBlank()) {
                "BILLING_SYNC:${_shopCode.value}:${_shopSecret.value}:${_supabaseUrl.value}:${_supabaseKey.value}:$pat:$ref"
            } else {
                "BILLING_SYNC:${_shopCode.value}:${_shopSecret.value}:${_supabaseUrl.value}:${_supabaseKey.value}"
            }
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
            _configQrBitmap.value = bitmap
        } catch (e: Exception) {
            Log.e("SettingsVM", "Config QR generation failed", e)
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

sealed class SyncStatus {
    data object Idle : SyncStatus()
    data object Syncing : SyncStatus()
    data object Connected : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}

sealed class JoinStatus {
    data object Idle : JoinStatus()
    data object Loading : JoinStatus()
    data object Success : JoinStatus()
    data class Error(val message: String) : JoinStatus()
}
