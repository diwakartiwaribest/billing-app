package com.shop.billing.ui.screens.dbmanager

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shop.billing.data.remote.SupabaseClient
import com.shop.billing.util.Constants
import com.shop.billing.util.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class DatabaseManagerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    private val _bills = MutableStateFlow<JSONArray>(JSONArray())
    val bills: StateFlow<JSONArray> = _bills

    private val _billItems = MutableStateFlow<JSONArray>(JSONArray())
    val billItems: StateFlow<JSONArray> = _billItems

    private val _shopItems = MutableStateFlow<JSONArray>(JSONArray())
    val shopItems: StateFlow<JSONArray> = _shopItems

    private val _members = MutableStateFlow<JSONArray>(JSONArray())
    val members: StateFlow<JSONArray> = _members

    private val _shops = MutableStateFlow<JSONArray>(JSONArray())
    val shops: StateFlow<JSONArray> = _shops

    private val _currentShopCode = MutableStateFlow("")
    val currentShopCode: StateFlow<String> = _currentShopCode

    private val _shopSettings = MutableStateFlow<JSONObject?>(null)
    val shopSettings: StateFlow<JSONObject?> = _shopSettings

    private val _customers = MutableStateFlow<JSONArray>(JSONArray())
    val customers: StateFlow<JSONArray> = _customers

    private val _payments = MutableStateFlow<JSONArray>(JSONArray())
    val payments: StateFlow<JSONArray> = _payments

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage

    private var url = ""
    private var key = ""
    private var code = ""

    init {
        viewModelScope.launch {
            val prefs = context.dataStore.data.first()
            url = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_URL)] ?: ""
            key = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_KEY)] ?: ""
            code = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: ""
            _currentShopCode.value = code
            loadAll()
        }
    }

    fun loadAll() {
        if (url.isBlank() || key.isBlank()) return
        loadShops()
        loadBills()
        loadBillItems()
        loadShopItems()
        loadMembers()
        loadShopSettings()
        loadCustomers()
        loadPayments()
    }

    fun loadBills() {
        viewModelScope.launch {
            try {
                _bills.value = withContext(Dispatchers.IO) {
                    supabaseClient.fetchAllBills(url, key, code)
                }
            } catch (e: Exception) { Log.e("DbMgrVM", "loadBills failed", e) }
        }
    }

    fun loadBillItems() {
        viewModelScope.launch {
            try {
                _billItems.value = withContext(Dispatchers.IO) {
                    supabaseClient.fetchAllBillItems(url, key, code)
                }
            } catch (e: Exception) { Log.e("DbMgrVM", "loadBillItems failed", e) }
        }
    }

    fun loadShopItems() {
        viewModelScope.launch {
            try {
                _shopItems.value = withContext(Dispatchers.IO) {
                    supabaseClient.fetchAllShopItems(url, key, code)
                }
            } catch (e: Exception) { Log.e("DbMgrVM", "loadShopItems failed", e) }
        }
    }

    fun loadMembers() {
        viewModelScope.launch {
            try {
                _members.value = withContext(Dispatchers.IO) {
                    supabaseClient.fetchAllUserShops(url, key, code)
                }
            } catch (e: Exception) { Log.e("DbMgrVM", "loadMembers failed", e) }
        }
    }

    fun loadShopSettings() {
        if (code.isBlank()) return
        viewModelScope.launch {
            try {
                _shopSettings.value = withContext(Dispatchers.IO) {
                    supabaseClient.fetchShopSettings(url, key, code)
                }
            } catch (e: Exception) { Log.e("DbMgrVM", "loadShopSettings failed", e) }
        }
    }

    fun loadCustomers() {
        viewModelScope.launch {
            try {
                _customers.value = withContext(Dispatchers.IO) {
                    supabaseClient.fetchAllCustomers(url, key, code)
                }
            } catch (e: Exception) { Log.e("DbMgrVM", "loadCustomers failed", e) }
        }
    }

    fun loadPayments() {
        viewModelScope.launch {
            try {
                _payments.value = withContext(Dispatchers.IO) {
                    val paymentsList = supabaseClient.pullCustomerPayments(url, key, code)
                    val jsonArray = JSONArray()
                    for (payment in paymentsList) {
                        jsonArray.put(JSONObject().apply {
                            put("customer_mobile", payment.customerMobile)
                            put("amount", payment.amount)
                        })
                    }
                    jsonArray
                }
            } catch (e: Exception) { Log.e("DbMgrVM", "loadPayments failed", e) }
        }
    }

    fun deleteCustomer(customerId: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    supabaseClient.deleteCustomer(url, key, customerId)
                }
                _statusMessage.value = "Customer deleted"
                loadCustomers()
            } catch (e: Exception) {
                _statusMessage.value = "Failed: ${e.message}"
            }
        }
    }

    fun loadShops() {
        viewModelScope.launch {
            try {
                _shops.value = withContext(Dispatchers.IO) {
                    supabaseClient.fetchAllShops(url, key)
                }
            } catch (e: Exception) { Log.e("DbMgrVM", "loadShops failed", e) }
        }
    }

    fun deleteShopByCode(shopCode: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    supabaseClient.deleteShopByCode(url, key, shopCode)
                }
                _statusMessage.value = "Shop '$shopCode' deleted"
                loadShops()
                loadBills()
                loadBillItems()
                loadShopItems()
                loadMembers()
                loadShopSettings()
                loadCustomers()
            } catch (e: Exception) {
                _statusMessage.value = "Failed: ${e.message}"
            }
        }
    }

    fun deleteBill(billId: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    supabaseClient.deleteBill(url, key, code, billId)
                }
                _statusMessage.value = "Bill deleted"
                loadBills()
                loadBillItems()
            } catch (e: Exception) {
                _statusMessage.value = "Failed: ${e.message}"
            }
        }
    }

    fun deleteShopItem(itemId: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    supabaseClient.deleteShopItem(url, key, code, itemId)
                }
                _statusMessage.value = "Item deleted"
                loadShopItems()
            } catch (e: Exception) {
                _statusMessage.value = "Failed: ${e.message}"
            }
        }
    }

    fun updateShopItem(itemId: String, name: String, price: Double, category: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    supabaseClient.updateShopItem(url, key, code, itemId, name, price, category)
                }
                _statusMessage.value = "Item updated"
                loadShopItems()
            } catch (e: Exception) {
                _statusMessage.value = "Failed: ${e.message}"
            }
        }
    }

    fun removeMember(userId: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    supabaseClient.removeMember(url, key, code, userId)
                }
                _statusMessage.value = "Member removed"
                loadMembers()
            } catch (e: Exception) {
                _statusMessage.value = "Failed: ${e.message}"
            }
        }
    }

    fun updateShopSetting(field: String, value: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    supabaseClient.updateShopSettings(url, key, code, field, value)
                }
                _statusMessage.value = "Setting updated"
                loadShopSettings()
            } catch (e: Exception) {
                _statusMessage.value = "Failed: ${e.message}"
            }
        }
    }

    fun clearStatus() {
        _statusMessage.value = null
    }
}
