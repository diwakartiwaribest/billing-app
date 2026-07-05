package com.shop.billing.ui.screens.recyclebin

import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shop.billing.data.model.Bill
import com.shop.billing.data.model.Customer
import com.shop.billing.data.model.CustomerPayment
import com.shop.billing.data.model.ShopItem
import com.shop.billing.data.remote.FirebaseClient
import com.shop.billing.data.sync.SyncEngine
import com.shop.billing.util.Constants
import com.shop.billing.util.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class RecycleBinState(
    val bills: List<Bill> = emptyList(),
    val customers: List<Customer> = emptyList(),
    val products: List<ShopItem> = emptyList(),
    val payments: List<CustomerPayment> = emptyList(),
    val loading: Boolean = false,
    val lastRestored: String? = null,
    val lastPurged: String? = null,
    val error: String? = null,
    val canManage: Boolean = false
)

@HiltViewModel
class RecycleBinViewModel @Inject constructor(
    private val firebaseClient: FirebaseClient,
    private val syncEngine: SyncEngine,
    @ApplicationContext private val context: Context
    ) : ViewModel() {

        private val TAG = "RecycleBinVM"

        private val _state = MutableStateFlow(RecycleBinState())
    val state: StateFlow<RecycleBinState> = _state

    private val _shopCode = MutableStateFlow("")
    val shopCode: StateFlow<String> = _shopCode

    init {
        viewModelScope.launch {
            try {
                val prefs = context.dataStore.data.first()
                _shopCode.value = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: ""
                _state.value = _state.value.copy(
                    canManage = (prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ROLE)] ?: "member") in setOf("owner", "admin")
                )
                refresh()
            } catch (e: Exception) {
                Log.e(TAG, "init failed", e)
                _state.value = _state.value.copy(error = e.message)
                _state.value = _state.value.copy(loading = false)
            }
        }
    }

    fun refresh() {
        val shop = _shopCode.value
        if (shop.isBlank()) return
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val bills = firebaseClient.listDeletedBills(shop)
                    val customers = firebaseClient.listDeletedCustomers(shop)
                    val products = firebaseClient.listDeletedShopItems(shop)
                    val payments = firebaseClient.listDeletedPayments(shop)
                    RecycleBinState(
                        bills = bills.sortedByDescending { it.updatedAt },
                        customers = customers.sortedByDescending { it.updatedAt },
                        products = products.sortedByDescending { it.updatedAt },
                        payments = payments.sortedByDescending { it.updatedAt },
                        loading = false,
                        canManage = _state.value.canManage
                    )
                }
                _state.value = result
            } catch (e: Exception) {
                Log.e(TAG, "refresh failed", e)
                _state.value = _state.value.copy(error = e.message, loading = false)
            }
        }
    }

    fun restoreBill(billId: String) {
        val shop = _shopCode.value
        viewModelScope.launch {
            try {
                val ok = syncEngine.restoreBill(shop, billId)
                if (ok) {
                    syncEngine.pushPending(shop)
                    _state.value = _state.value.copy(lastRestored = "Bill restored")
                    refresh()
                } else {
                    _state.value = _state.value.copy(error = "Could not restore bill")
                }
            } catch (e: Exception) {
                Log.e(TAG, "restoreBill failed", e)
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun restoreCustomer(mobile: String) {
        val shop = _shopCode.value
        viewModelScope.launch {
            try {
                val ok = syncEngine.restoreCustomer(shop, mobile)
                if (ok) {
                    syncEngine.pushPending(shop)
                    _state.value = _state.value.copy(lastRestored = "Customer restored")
                    refresh()
                } else {
                    _state.value = _state.value.copy(error = "Could not restore customer")
                }
            } catch (e: Exception) {
                Log.e(TAG, "restoreCustomer failed", e)
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun restoreProduct(id: String) {
        val shop = _shopCode.value
        viewModelScope.launch {
            try {
                val ok = syncEngine.restoreShopItem(shop, id)
                if (ok) {
                    syncEngine.pushPending(shop)
                    _state.value = _state.value.copy(lastRestored = "Item restored")
                    refresh()
                } else {
                    _state.value = _state.value.copy(error = "Could not restore item")
                }
            } catch (e: Exception) {
                Log.e(TAG, "restoreProduct failed", e)
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun restorePayment(uuid: String) {
        val shop = _shopCode.value
        viewModelScope.launch {
            try {
                val ok = syncEngine.restoreCustomerPayment(shop, uuid)
                if (ok) {
                    syncEngine.pushPending(shop)
                    _state.value = _state.value.copy(lastRestored = "Payment restored")
                    refresh()
                } else {
                    _state.value = _state.value.copy(error = "Could not restore payment")
                }
            } catch (e: Exception) {
                Log.e(TAG, "restorePayment failed", e)
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
