package com.shop.billing.data.sync

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.shop.billing.data.local.dao.CustomerDao
import com.shop.billing.data.local.dao.CustomerPaymentDao
import com.shop.billing.data.local.dao.InvoiceDao
import com.shop.billing.data.local.dao.InvoiceItemDao
import com.shop.billing.data.local.dao.ProductDao
import com.shop.billing.data.local.entity.SyncStatus
import com.shop.billing.data.local.entity.toEntity
import com.shop.billing.data.remote.FirebaseClient
import com.shop.billing.data.sync.OperationType
import com.shop.billing.util.Constants
import com.shop.billing.util.dataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SyncEngine"

@Singleton
class SyncEngine @Inject constructor(
    private val firebaseClient: FirebaseClient,
    private val productDao: ProductDao,
    private val customerDao: CustomerDao,
    private val invoiceDao: InvoiceDao,
    private val invoiceItemDao: InvoiceItemDao,
    private val customerPaymentDao: CustomerPaymentDao,
    @ApplicationContext private val context: Context
) {
    private val _logEntries = MutableStateFlow<List<LogEntry>>(emptyList())
    val logEntries: StateFlow<List<LogEntry>> = _logEntries

    private val _showLog = MutableStateFlow(true)
    val showLog: StateFlow<Boolean> = _showLog

    private val logTimeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun addLog(message: String, type: LogType = LogType.INFO) {
        val now = Date()
        val entry = LogEntry(timestamp = logTimeFormat.format(now), message = message, type = type, timestampMillis = now.time)
        val updated = _logEntries.value + entry
        val cutoff = now.time - 24 * 60 * 60 * 1000L
        _logEntries.value = updated.filter { it.timestampMillis >= cutoff }.takeLast(500)
    }

    fun clearLog() { _logEntries.value = emptyList() }

    fun toggleLog() { _showLog.value = !_showLog.value }
    suspend fun pushPending(shopCode: String) {
        addLog("Syncing local changes to Firebase...", LogType.INFO)
        withContext(Dispatchers.IO) {
            pushProducts(shopCode)
            pushCustomers(shopCode)
            pushInvoices(shopCode)
            pushPayments(shopCode)
        }
        addLog("Local sync completed", LogType.SUCCESS)
        Log.e(TAG, "pushPending completed")
    }

    suspend fun exportAllToFirebase(shopCode: String): String {
        var errors = 0
        var total = 0
        val details = mutableListOf<String>()
        addLog("Starting full export to Firebase...", LogType.INFO)
        withContext(Dispatchers.IO) {
            Log.i(TAG, "exportAllToFirebase: starting export for shop $shopCode")

            val customers = customerDao.getAllNoFilter()
            details.add("customers=${customers.size}")
            total += customers.size
            for (entity in customers) {
                try {
                    val opType = when {
                        entity.deleted -> OperationType.DELETE
                        entity.syncStatus == SyncStatus.SYNCED || entity.syncStatus == SyncStatus.PENDING_UPDATE -> OperationType.UPDATE
                        else -> OperationType.CREATE
                    }
                    firebaseClient.pushCustomer(shopCode, entity.toCustomer(), opType)
                    customerDao.updateSyncStatus(entity.mobile, SyncStatus.SYNCED, null)
                } catch (e: Exception) {
                    Log.e(TAG, "export customer ${entity.mobile} failed", e)
                    errors++
                }
            }

            val payments = customerPaymentDao.getAllNoFilter()
            details.add("payments=${payments.size}")
            total += payments.size
            for (entity in payments) {
                try {
                    val opType = when {
                        entity.deleted -> OperationType.DELETE
                        entity.syncStatus == SyncStatus.SYNCED || entity.syncStatus == SyncStatus.PENDING_UPDATE -> OperationType.UPDATE
                        else -> OperationType.CREATE
                    }
                    firebaseClient.pushCustomerPayment(shopCode, entity.toCustomerPayment(), opType)
                    customerPaymentDao.updateSyncStatus(entity.uuid, SyncStatus.SYNCED, null)
                } catch (e: Exception) {
                    Log.e(TAG, "export payment ${entity.uuid} failed", e)
                    errors++
                }
            }

            val products = productDao.getAllNoFilter()
            details.add("products=${products.size}")
            total += products.size
            for (entity in products) {
                try {
                    val opType = when {
                        entity.deleted -> OperationType.DELETE
                        entity.syncStatus == SyncStatus.SYNCED || entity.syncStatus == SyncStatus.PENDING_UPDATE -> OperationType.UPDATE
                        else -> OperationType.CREATE
                    }
                    firebaseClient.pushShopItem(shopCode, entity.toShopItem(), opType)
                    productDao.updateSyncStatus(entity.id, SyncStatus.SYNCED, null)
                } catch (e: Exception) {
                    Log.e(TAG, "export product ${entity.id} failed", e)
                    errors++
                }
            }

            val invoices = invoiceDao.getAllNoFilter()
            details.add("invoices=${invoices.size}")
            total += invoices.size
            for (entity in invoices) {
                try {
                    val opType = when {
                        entity.deleted -> OperationType.DELETE
                        entity.syncStatus == SyncStatus.SYNCED || entity.syncStatus == SyncStatus.PENDING_UPDATE -> OperationType.UPDATE
                        else -> OperationType.CREATE
                    }
                    val items = invoiceItemDao.getByInvoiceAll(entity.id)
                    firebaseClient.pushBill(shopCode, entity.toBill(), items.map { it.toBillItem() }, opType)
                    invoiceDao.updateSyncStatus(entity.id, SyncStatus.SYNCED, null)
                    for (item in items) {
                        if (item.syncStatus != SyncStatus.SYNCED) {
                            invoiceItemDao.updateSyncStatus(item.id, SyncStatus.SYNCED, null)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "export invoice ${entity.id} failed", e)
                    errors++
                }
            }

            Log.i(TAG, "exportAllToFirebase: complete - $total items, $errors errors [${details.joinToString(", ")}]")
        }

        withContext(Dispatchers.IO) {
            customerDao.updateShopCode(shopCode)
            customerPaymentDao.updateShopCode(shopCode)
            productDao.updateShopCode(shopCode)
            invoiceDao.updateShopCode(shopCode)
            invoiceItemDao.updateShopCode(shopCode)
            Log.i(TAG, "exportAllToFirebase: updated shopCode in Room to $shopCode")
        }

        return if (errors == 0) "Exported $total items: ${details.joinToString(", ")}" else "Exported ${total - errors}/$total items ($errors errors): ${details.joinToString(", ")}"
    }

    private suspend fun pushProducts(shopCode: String) {
        val pending = productDao.getPendingSync(shopCode)
        if (pending.isNotEmpty()) addLog("Pushing ${pending.size} item(s)...", LogType.INFO)
        for (entity in pending) {
            try {
                val opType = when (entity.syncStatus) {
                    SyncStatus.PENDING_CREATE -> OperationType.CREATE
                    SyncStatus.PENDING_UPDATE -> OperationType.UPDATE
                    SyncStatus.PENDING_DELETE -> OperationType.DELETE
                    SyncStatus.FAILED -> OperationType.UPDATE
                    else -> continue
                }
                firebaseClient.pushShopItem(shopCode, entity.toShopItem(), opType)
                productDao.updateSyncStatus(entity.id, SyncStatus.SYNCED, null)
                addLog("Item '${entity.name}' ${opType.name.lowercase()} synced", LogType.SUCCESS)
            } catch (e: Exception) {
                Log.e(TAG, "push product ${entity.id} failed", e)
                productDao.updateSyncStatus(entity.id, SyncStatus.FAILED, e.message)
                addLog("Item '${entity.name}' sync failed: ${e.message}", LogType.ERROR)
            }
        }
    }

    private suspend fun pushCustomers(shopCode: String) {
        val pending = customerDao.getPendingSync(shopCode)
        if (pending.isNotEmpty()) addLog("Pushing ${pending.size} customer(s)...", LogType.INFO)
        for (entity in pending) {
            try {
                val opType = when (entity.syncStatus) {
                    SyncStatus.PENDING_CREATE -> OperationType.CREATE
                    SyncStatus.PENDING_UPDATE -> OperationType.UPDATE
                    SyncStatus.PENDING_DELETE -> OperationType.DELETE
                    SyncStatus.FAILED -> OperationType.UPDATE
                    else -> continue
                }
                firebaseClient.pushCustomer(shopCode, entity.toCustomer(), opType)
                customerDao.updateSyncStatus(entity.mobile, SyncStatus.SYNCED, null)
                addLog("Customer '${entity.name}' ${opType.name.lowercase()} synced", LogType.SUCCESS)
            } catch (e: Exception) {
                Log.e(TAG, "push customer ${entity.mobile} failed", e)
                customerDao.updateSyncStatus(entity.mobile, SyncStatus.FAILED, e.message)
                addLog("Customer '${entity.name}' sync failed: ${e.message}", LogType.ERROR)
            }
        }
    }

    private suspend fun pushInvoices(shopCode: String) {
        val pending = invoiceDao.getPendingSync(shopCode)
        val pendingItems = invoiceItemDao.getPendingSync(shopCode).filter { it.syncStatus != SyncStatus.SYNCED }
        val totalItems = pendingItems.size + pending.size
        if (totalItems > 0) addLog("Pushing ${pending.size} bill(s) and ${pendingItems.size} item(s)...", LogType.INFO)
        for (entity in pending) {
            try {
                val opType = when (entity.syncStatus) {
                    SyncStatus.PENDING_CREATE -> OperationType.CREATE
                    SyncStatus.PENDING_UPDATE -> OperationType.UPDATE
                    SyncStatus.PENDING_DELETE -> OperationType.DELETE
                    SyncStatus.FAILED -> OperationType.UPDATE
                    else -> continue
                }
                val items = invoiceItemDao.getByInvoiceAll(entity.id)
                firebaseClient.pushBill(shopCode, entity.toBill(), items.map { it.toBillItem() }, opType)
                invoiceDao.updateSyncStatus(entity.id, SyncStatus.SYNCED, null)
                for (item in items) {
                    if (item.syncStatus != SyncStatus.SYNCED) {
                        invoiceItemDao.updateSyncStatus(item.id, SyncStatus.SYNCED, null)
                    }
                }
                val customerInfo = if (entity.customerName.isNotBlank()) " for ${entity.customerName}" else ""
                addLog("Bill #${entity.billNumber}$customerInfo ${opType.name.lowercase()} synced", LogType.SUCCESS)
            } catch (e: Exception) {
                Log.e(TAG, "push invoice ${entity.id} failed", e)
                invoiceDao.updateSyncStatus(entity.id, SyncStatus.FAILED, e.message)
                addLog("Bill #${entity.billNumber} sync failed: ${e.message}", LogType.ERROR)
            }
        }

        for (item in pendingItems) {
            try {
                val parent = invoiceDao.getById(item.invoiceId)
                if (parent == null) {
                    invoiceItemDao.updateSyncStatus(item.id, SyncStatus.FAILED, "no parent invoice")
                    continue
                }
                val opType = when (item.syncStatus) {
                    SyncStatus.PENDING_CREATE -> OperationType.CREATE
                    SyncStatus.PENDING_UPDATE -> OperationType.UPDATE
                    SyncStatus.PENDING_DELETE -> OperationType.DELETE
                    SyncStatus.FAILED -> OperationType.UPDATE
                    else -> continue
                }
                val parentBill = parent.toBill()
                val allItems = invoiceItemDao.getByInvoiceAll(item.invoiceId).map { it.toBillItem() }
                firebaseClient.pushBill(shopCode, parentBill, allItems, if (opType == OperationType.DELETE) OperationType.UPDATE else opType)
                invoiceItemDao.updateSyncStatus(item.id, SyncStatus.SYNCED, null)
                addLog("Bill item '${item.itemName}' in #${parent.billNumber} synced", LogType.SUCCESS)
            } catch (e: Exception) {
                Log.e(TAG, "push invoice item ${item.id} failed", e)
                invoiceItemDao.updateSyncStatus(item.id, SyncStatus.FAILED, e.message)
                addLog("Bill item sync failed: ${e.message}", LogType.ERROR)
            }
        }
    }

    private suspend fun pushPayments(shopCode: String) {
        val pending = customerPaymentDao.getPendingSync(shopCode)
        if (pending.isNotEmpty()) addLog("Pushing ${pending.size} payment(s)...", LogType.INFO)
        for (entity in pending) {
            try {
                val opType = when (entity.syncStatus) {
                    SyncStatus.PENDING_CREATE -> OperationType.CREATE
                    SyncStatus.PENDING_UPDATE -> OperationType.UPDATE
                    SyncStatus.PENDING_DELETE -> OperationType.DELETE
                    SyncStatus.FAILED -> OperationType.UPDATE
                    else -> continue
                }
                firebaseClient.pushCustomerPayment(shopCode, entity.toCustomerPayment(), opType)
                customerPaymentDao.updateSyncStatus(entity.uuid, SyncStatus.SYNCED, null)
                addLog("Payment ₹${entity.amount} for ${entity.customerMobile} ${opType.name.lowercase()} synced", LogType.SUCCESS)
            } catch (e: Exception) {
                Log.e(TAG, "push payment ${entity.uuid} failed", e)
                customerPaymentDao.updateSyncStatus(entity.uuid, SyncStatus.FAILED, e.message)
                addLog("Payment for ${entity.customerMobile} sync failed: ${e.message}", LogType.ERROR)
            }
        }
    }

    private var realtimeScope: CoroutineScope? = null
    private var realtimeJob: Job? = null

    fun startRealtimeSync(shopCode: String, scope: CoroutineScope) {
        stopRealtimeSync()
        realtimeScope = scope
        realtimeJob = scope.launch {
            Log.i(TAG, "startRealtimeSync: starting for shop $shopCode")
            addLog("Real-time sync started for shop $shopCode", LogType.INFO)

            launch { collectCustomers(shopCode) }
            launch { collectShopItems(shopCode) }
            launch { collectBills(shopCode) }
            launch { collectBillItems(shopCode) }
            launch { collectPayments(shopCode) }
            launch { collectShopInfo(shopCode) }
        }
    }

    fun stopRealtimeSync() {
        realtimeJob?.cancel()
        realtimeJob = null
        realtimeScope = null
        firebaseClient.unsubscribeAll()
    }

    private var lastCustomerCount = -1
    private suspend fun collectCustomers(shopCode: String) {
        try {
            Log.i(TAG, "collectCustomers: listening for shop $shopCode")
            firebaseClient.subscribeToCustomers(shopCode).collect { customers ->
                Log.i(TAG, "collectCustomers: received ${customers.size} customers")
                val count = customers.size
                if (count != lastCustomerCount) {
                    addLog("$count customer(s) from remote", LogType.INFO)
                    lastCustomerCount = count
                }
                withContext(Dispatchers.IO) {
                    customers.forEach { customer ->
                        try {
                            customerDao.upsert(customer.toEntity(shopCode))
                        } catch (e: Exception) {
                            Log.e(TAG, "collectCustomers upsert failed for ${customer.mobile}", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "collectCustomers failed", e)
        }
    }

    private var lastItemCount = -1
    private suspend fun collectShopItems(shopCode: String) {
        try {
            firebaseClient.subscribeToShopItems(shopCode).collect { items ->
                val count = items.size
                if (count != lastItemCount) {
                    addLog("$count shop item(s) from remote", LogType.INFO)
                    lastItemCount = count
                }
                withContext(Dispatchers.IO) {
                    items.forEach { item ->
                        try {
                            productDao.upsert(item.toEntity(shopCode))
                        } catch (e: Exception) {
                            Log.e(TAG, "collectShopItems upsert failed for ${item.id}", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "collectShopItems failed", e)
        }
    }

    private var lastBillCount = -1
    private suspend fun collectBills(shopCode: String) {
        try {
            firebaseClient.subscribeToBills(shopCode).collect { bills ->
                val count = bills.size
                if (count != lastBillCount) {
                    addLog("$count bill(s) from remote", LogType.INFO)
                    lastBillCount = count
                }
                withContext(Dispatchers.IO) {
                    bills.forEach { bill ->
                        try {
                            invoiceDao.upsert(bill.toEntity(shopCode))
                        } catch (e: Exception) {
                            Log.e(TAG, "collectBills upsert failed for ${bill.id}", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "collectBills failed", e)
        }
    }

    private var lastBillItemCount = -1
    private suspend fun collectBillItems(shopCode: String) {
        try {
            firebaseClient.subscribeToBillItems(shopCode).collect { items ->
                val count = items.size
                if (count != lastBillItemCount) {
                    addLog("$count bill item(s) from remote", LogType.INFO)
                    lastBillItemCount = count
                }
                withContext(Dispatchers.IO) {
                    items.forEach { item ->
                        try {
                            invoiceItemDao.upsert(item.toEntity(shopCode))
                        } catch (e: Exception) {
                            Log.e(TAG, "collectBillItems upsert failed for ${item.id}", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "collectBillItems failed", e)
        }
    }

    private var lastPaymentCount = -1
    private suspend fun collectPayments(shopCode: String) {
        try {
            firebaseClient.subscribeToPayments(shopCode).collect { payments ->
                val count = payments.size
                if (count != lastPaymentCount) {
                    addLog("$count payment(s) from remote", LogType.INFO)
                    lastPaymentCount = count
                }
                withContext(Dispatchers.IO) {
                    payments.forEach { payment ->
                        try {
                            customerPaymentDao.upsert(payment.toEntity(shopCode))
                        } catch (e: Exception) {
                            Log.e(TAG, "collectPayments upsert failed for ${payment.uuid}", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "collectPayments failed", e)
        }
    }

    private var shopInfoVersion = -1L
    private suspend fun collectShopInfo(shopCode: String) {
        try {
            firebaseClient.subscribeToShopInfo(shopCode).collect { data ->
                val updatedAt = (data["updatedAt"] as? Long) ?: 0L
                if (updatedAt > shopInfoVersion) {
                    addLog("Shop settings updated from remote", LogType.INFO)
                    shopInfoVersion = updatedAt
                }
                withContext(Dispatchers.IO) {
                    try {
                        context.dataStore.edit { prefs ->
                            data["name"]?.toString()?.takeIf { it.isNotBlank() }?.let {
                                prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_NAME)] = it
                            }
                            data["address"]?.toString()?.takeIf { it.isNotBlank() }?.let {
                                prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_ADDRESS)] = it
                            }
                            data["phone"]?.toString()?.takeIf { it.isNotBlank() }?.let {
                                prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_PHONE)] = it
                            }
                            if (data.containsKey("invoiceMessage")) {
                                prefs[stringPreferencesKey(Constants.SETTINGS_KEY_INVOICE_MESSAGE)] = data["invoiceMessage"]?.toString() ?: ""
                            }
                            data["logo"]?.toString()?.takeIf { it.isNotBlank() }?.let {
                                prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_LOGO)] = it
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "collectShopInfo edit failed", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "collectShopInfo failed", e)
        }
    }
}
