package com.shop.billing.ui.screens.recyclebin

import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shop.billing.data.model.Bill
import com.shop.billing.data.model.Customer
import com.shop.billing.data.model.CustomerPayment
import com.shop.billing.data.model.ShopItem
import com.shop.billing.data.local.entity.toEntity
import com.shop.billing.data.remote.FirebaseClient
import com.shop.billing.data.repository.CustomerPaymentRepository
import com.shop.billing.data.repository.CustomerRepository
import com.shop.billing.data.repository.InvoiceRepository
import com.shop.billing.data.repository.ProductRepository
import com.shop.billing.data.sync.SyncEngine
import com.shop.billing.util.Constants
import com.shop.billing.util.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import android.util.Log
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import javax.inject.Inject

data class RecycleBinState(
    val bills: List<Bill> = emptyList(),
    val customers: List<Customer> = emptyList(),
    val products: List<ShopItem> = emptyList(),
    val payments: List<CustomerPayment> = emptyList(),
    val canManage: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val selectionMode: Boolean = false,
    val lastRestored: String? = null,
    val error: String? = null
)

@HiltViewModel
class RecycleBinViewModel @Inject constructor(
    private val firebaseClient: FirebaseClient,
    private val syncEngine: SyncEngine,
    private val productRepository: ProductRepository,
    private val customerRepository: CustomerRepository,
    private val invoiceRepository: InvoiceRepository,
    private val paymentRepository: CustomerPaymentRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val TAG = "RecycleBinVM"

    private val _state = MutableStateFlow(RecycleBinState())
    val state: StateFlow<RecycleBinState> = _state

    private var shopCode = ""

    init {
        viewModelScope.launch {
            try {
                val prefs = context.dataStore.data.first()
                shopCode = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: ""
                _state.value = _state.value.copy(
                    canManage = (prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ROLE)] ?: "member") in setOf("owner", "admin")
                )
                if (shopCode.isNotBlank()) {
                    launch {
                        productRepository.observeDeleted(shopCode).collect { entities ->
                            _state.value = _state.value.copy(products = entities.map { it.toShopItem() })
                        }
                    }
                    launch {
                        invoiceRepository.observeDeleted(shopCode).collect { entities ->
                            _state.value = _state.value.copy(bills = entities.map { it.toBill() })
                        }
                    }
                    launch {
                        customerRepository.observeDeleted(shopCode).collect { entities ->
                            _state.value = _state.value.copy(customers = entities.map { it.toCustomer() })
                        }
                    }
                    launch {
                        paymentRepository.observeDeleted(shopCode).collect { entities ->
                            _state.value = _state.value.copy(payments = entities.map { it.toCustomerPayment() })
                        }
                    }
                    pullFromFirebaseInBackground()
                }
            } catch (e: Exception) {
                Log.e(TAG, "init failed", e)
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    private suspend fun pullFromFirebaseInBackground() {
        try {
            val shop = shopCode
            if (shop.isBlank()) return
            withContext(Dispatchers.IO) {
                val bills = firebaseClient.listDeletedBills(shop)
                val customers = firebaseClient.listDeletedCustomers(shop)
                val products = firebaseClient.listDeletedShopItems(shop)
                val payments = firebaseClient.listDeletedPayments(shop)
                bills.forEach { invoiceRepository.upsertAll(listOf(it.toEntity(shop))) }
                customers.forEach { customerRepository.upsertAll(listOf(it.toEntity(shop))) }
                products.forEach { productRepository.upsertAll(listOf(it.toEntity(shop))) }
                payments.forEach { paymentRepository.upsertAll(listOf(it.toEntity(shop))) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "pullFromFirebaseInBackground failed", e)
        }
    }

    fun enterSelectionMode(initialId: String? = null) {
        val newSelection = if (initialId != null) setOf(initialId) else emptySet()
        _state.value = _state.value.copy(selectionMode = true, selectedIds = newSelection)
    }

    fun toggleSelection(id: String) {
        val current = _state.value.selectedIds
        val updated = if (id in current) current - id else current + id
        _state.value = _state.value.copy(
            selectionMode = updated.isNotEmpty(),
            selectedIds = updated
        )
    }

    fun selectAllInCurrentTab(tabIndex: Int) {
        val ids = when (tabIndex) {
            0 -> _state.value.bills.map { it.id }
            1 -> _state.value.products.map { it.id }
            2 -> _state.value.customers.map { it.mobile }
            3 -> _state.value.payments.map { it.uuid }
            else -> emptyList()
        }
        if (ids.isNotEmpty()) {
            _state.value = _state.value.copy(
                selectionMode = true,
                selectedIds = _state.value.selectedIds + ids
            )
        }
    }

    fun clearSelection() {
        _state.value = _state.value.copy(selectionMode = false, selectedIds = emptySet())
    }

    fun restoreProduct(id: String) {
        restoreProducts(listOf(id))
    }

    fun restoreProducts(ids: List<String>) {
        if (ids.isEmpty()) return
        val shop = shopCode
        viewModelScope.launch(Dispatchers.IO) {
            try {
                ids.forEach { syncEngine.restoreShopItem(shop, it) }
                withContext(NonCancellable) { syncEngine.pushPending(shop) }
                val msg = if (ids.size == 1) "Item restored" else "${ids.size} items restored"
                withContext(Dispatchers.Main) {
                    val s = _state.value
                    val remaining = s.selectedIds - ids.toSet()
                    _state.value = s.copy(
                        products = s.products.filterNot { p -> p.id in ids },
                        lastRestored = msg,
                        selectedIds = remaining,
                        selectionMode = remaining.isNotEmpty()
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "restoreProducts failed", e)
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun restoreBill(id: String) {
        restoreBills(listOf(id))
    }

    fun restoreBills(ids: List<String>) {
        if (ids.isEmpty()) return
        val shop = shopCode
        viewModelScope.launch(Dispatchers.IO) {
            try {
                ids.forEach { syncEngine.restoreBill(shop, it) }
                withContext(NonCancellable) { syncEngine.pushPending(shop) }
                val msg = if (ids.size == 1) "Bill restored" else "${ids.size} bills restored"
                withContext(Dispatchers.Main) {
                    val s = _state.value
                    val remaining = s.selectedIds - ids.toSet()
                    _state.value = s.copy(
                        bills = s.bills.filterNot { b -> b.id in ids },
                        lastRestored = msg,
                        selectedIds = remaining,
                        selectionMode = remaining.isNotEmpty()
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "restoreBills failed", e)
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun restoreCustomer(mobile: String) {
        restoreCustomers(listOf(mobile))
    }

    fun restoreCustomers(mobiles: List<String>) {
        if (mobiles.isEmpty()) return
        val shop = shopCode
        viewModelScope.launch(Dispatchers.IO) {
            try {
                mobiles.forEach { syncEngine.restoreCustomer(shop, it) }
                withContext(NonCancellable) { syncEngine.pushPending(shop) }
                val msg = if (mobiles.size == 1) "Customer restored" else "${mobiles.size} customers restored"
                withContext(Dispatchers.Main) {
                    val s = _state.value
                    val mobileSet = mobiles.toSet()
                    val remaining = s.selectedIds - mobileSet
                    _state.value = s.copy(
                        customers = s.customers.filterNot { c -> c.mobile in mobileSet },
                        lastRestored = msg,
                        selectedIds = remaining,
                        selectionMode = remaining.isNotEmpty()
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "restoreCustomers failed", e)
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun restorePayment(uuid: String) {
        restorePayments(listOf(uuid))
    }

    fun restorePayments(uuids: List<String>) {
        if (uuids.isEmpty()) return
        val shop = shopCode
        viewModelScope.launch(Dispatchers.IO) {
            try {
                uuids.forEach { syncEngine.restoreCustomerPayment(shop, it) }
                withContext(NonCancellable) { syncEngine.pushPending(shop) }
                val msg = if (uuids.size == 1) "Payment restored" else "${uuids.size} payments restored"
                withContext(Dispatchers.Main) {
                    val s = _state.value
                    val uuidSet = uuids.toSet()
                    val remaining = s.selectedIds - uuidSet
                    _state.value = s.copy(
                        payments = s.payments.filterNot { p -> p.uuid in uuidSet },
                        lastRestored = msg,
                        selectedIds = remaining,
                        selectionMode = remaining.isNotEmpty()
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "restorePayments failed", e)
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun clearLastRestored() {
        _state.value = _state.value.copy(lastRestored = null)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
