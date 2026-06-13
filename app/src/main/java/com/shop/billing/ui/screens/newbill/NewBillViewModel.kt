package com.shop.billing.ui.screens.newbill

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shop.billing.data.model.Bill
import com.shop.billing.data.model.BillItem
import com.shop.billing.data.model.Customer
import com.shop.billing.data.model.ShopItem
import com.shop.billing.data.remote.SupabaseClient
import com.shop.billing.data.remote.SyncManager
import com.shop.billing.data.repository.BillRepository
import com.shop.billing.data.repository.ShopItemRepository
import com.shop.billing.ui.components.CartItem
import com.shop.billing.util.Constants
import com.shop.billing.util.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class NewBillViewModel @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val syncManager: SyncManager,
    private val billRepository: BillRepository,
    private val shopItemRepository: ShopItemRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    private val _customerSearchQuery = MutableStateFlow("")
    val customerSearchQuery: StateFlow<String> = _customerSearchQuery

    private val _allCustomers = MutableStateFlow<List<Customer>>(emptyList())

    private val _filteredCustomers = MutableStateFlow<List<Customer>>(emptyList())
    val filteredCustomers: StateFlow<List<Customer>> = _filteredCustomers

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems

    private val _paymentStatus = MutableStateFlow("paid")
    val paymentStatus: StateFlow<String> = _paymentStatus

    fun onPaymentStatusChange(status: String) {
        _paymentStatus.value = status
    }

    private val _allItems = MutableStateFlow<List<ShopItem>>(emptyList())

    init {
        viewModelScope.launch {
            try {
                val prefs = context.dataStore.data.first()
                val url = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_URL)] ?: Constants.HARDCODED_SUPABASE_URL
                val key = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_KEY)] ?: Constants.HARDCODED_SUPABASE_KEY
                val shopCode = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: ""
                if (url.isNotBlank() && key.isNotBlank() && shopCode.isNotBlank()) {
                    _allCustomers.value = supabaseClient.pullCustomers(url, key, shopCode)
                }
            } catch (e: Exception) {
                Log.e("NewBillVM", "Failed to load customers from Supabase", e)
            }
        }
        viewModelScope.launch {
            shopItemRepository.getAllItems().collect { items ->
                _allItems.value = items
            }
        }
        pullItemsFromSupabase()
    }

    private fun pullItemsFromSupabase() {
        viewModelScope.launch {
            try {
                val prefs = context.dataStore.data.first()
                val url = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_URL)] ?: Constants.HARDCODED_SUPABASE_URL
                val key = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_KEY)] ?: Constants.HARDCODED_SUPABASE_KEY
                val code = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: return@launch
                val items = supabaseClient.pullShopItems(url, key, code)
                shopItemRepository.insertItems(items)
            } catch (_: Exception) {}
        }
    }

    fun onCustomerSearchQueryChange(query: String) {
        _customerSearchQuery.value = query
        if (query.isBlank()) {
            _filteredCustomers.value = emptyList()
            return
        }
        val q = query.trim().lowercase()
        _filteredCustomers.value = _allCustomers.value.filter {
            it.name.lowercase().contains(q) || it.mobile.contains(q)
        }.take(5)
    }

    fun selectCustomer(customer: Customer) {
        _customerSearchQuery.value = ""
        _filteredCustomers.value = emptyList()
    }

    val totalAmount: StateFlow<Double> = _cartItems
        .map { items -> items.sumOf { it.subtotal } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val categories: StateFlow<List<String>> = _allItems
        .map { items -> items.map { it.category }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val items: StateFlow<List<ShopItem>> = combine(
        _searchQuery,
        _selectedCategory,
        _allItems
    ) { query, category, allItems ->
        allItems.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.name.contains(query, ignoreCase = true) ||
                    item.category.contains(query, ignoreCase = true)
            val matchesCategory = category == null || item.category == category
            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: String?) {
        _selectedCategory.value = category
    }

    fun addToCart(item: ShopItem) {
        val current = _cartItems.value.toMutableList()
        val existing = current.find { it.itemId == item.id }
        if (existing != null) {
            current.replaceAll { it.copy(quantity = if (it.itemId == item.id) it.quantity + 1 else it.quantity) }
        } else {
            current.add(CartItem(itemId = item.id, itemName = item.name, unitPrice = item.price))
        }
        _cartItems.value = current
    }

    fun increaseQuantity(itemId: String) {
        _cartItems.value = _cartItems.value.map {
            if (it.itemId == itemId) it.copy(quantity = it.quantity + 1) else it
        }
    }

    fun decreaseQuantity(itemId: String) {
        _cartItems.value = _cartItems.value.mapNotNull {
            if (it.itemId == itemId) {
                if (it.quantity > 1) it.copy(quantity = it.quantity - 1) else null
            } else it
        }
    }

    fun removeFromCart(itemId: String) {
        _cartItems.value = _cartItems.value.filter { it.itemId != itemId }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    private suspend fun getShopCode(): String {
        return try {
            val prefs = context.dataStore.data.first()
            prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: ""
        } catch (_: Exception) { "" }
    }

    private suspend fun getSupabaseConfig(): Pair<String, String> {
        return try {
            val prefs = context.dataStore.data.first()
            val url = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_URL)] ?: ""
            val key = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_KEY)] ?: ""
            Pair(url, key)
        } catch (_: Exception) { Pair("", "") }
    }

    private suspend fun getUserEmail(): String {
        return try {
            val prefs = context.dataStore.data.first()
            prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_EMAIL)] ?: ""
        } catch (_: Exception) { "" }
    }

    fun generateBill(customerName: String, customerMobile: String, onResult: (String) -> Unit) {
        val cart = _cartItems.value
        Log.d("NewBillVM", "generateBill called, cart size=${cart.size}")
        if (cart.isEmpty()) {
            Log.d("NewBillVM", "Cart is empty, showing toast")
            Toast.makeText(context, "Cart is empty", Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch {
            try {
                Log.d("NewBillVM", "Creating bill with ${cart.size} items")
                val billId = UUID.randomUUID().toString()
                val total = cart.sumOf { it.subtotal }
                val prefs = context.dataStore.data.first()
                val shopName = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_NAME)] ?: Constants.DEFAULT_SHOP_NAME
                val initials = shopName.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(4).joinToString("")
                val now = java.util.Calendar.getInstance()
                val dd = "%02d".format(now.get(java.util.Calendar.DAY_OF_MONTH))
                val mm = "%02d".format(now.get(java.util.Calendar.MONTH) + 1)
                val yyyy = "%04d".format(now.get(java.util.Calendar.YEAR))
                val hh = "%02d".format(now.get(java.util.Calendar.HOUR_OF_DAY))
                val min = "%02d".format(now.get(java.util.Calendar.MINUTE))
                val ss = "%02d".format(now.get(java.util.Calendar.SECOND))
                val billNumber = "$initials$dd$mm$yyyy$hh$min$ss"
                val bill = Bill(
                    id = billId,
                    billNumber = billNumber,
                    customerName = customerName,
                    customerMobile = customerMobile,
                    totalAmount = total,
                    createdBy = getUserEmail(),
                    paymentStatus = _paymentStatus.value
                )
                val billItems = cart.map {
                    BillItem(
                        id = UUID.randomUUID().toString(),
                        billId = billId,
                        itemName = it.itemName,
                        quantity = it.quantity,
                        unitPrice = it.unitPrice,
                        subtotal = it.subtotal
                    )
                }

                billRepository.insertBillWithItems(bill, billItems)

                val (url, key) = getSupabaseConfig()
                val shopCode = getShopCode()
                withContext(Dispatchers.IO) {
                    syncManager.pushBillToSupabase(url, key, shopCode, bill, billItems)
                }
                Log.d("NewBillVM", "Bill saved to Room and pushed to Supabase with id=$billId")
                onResult(billId)
                clearCart()
            } catch (e: Exception) {
                Log.e("NewBillVM", "Error generating bill", e)
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }
}
