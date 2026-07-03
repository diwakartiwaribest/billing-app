package com.shop.billing.ui.screens.newbill

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shop.billing.data.model.Bill
import com.shop.billing.data.model.BillItem
import com.shop.billing.data.model.Customer
import com.shop.billing.data.model.ShopItem
import com.shop.billing.data.repository.CustomerRepository
import com.shop.billing.data.repository.InvoiceRepository
import com.shop.billing.data.repository.ProductRepository
import com.shop.billing.data.sync.SyncEngine
import com.shop.billing.ui.components.CartItem
import com.shop.billing.util.Constants
import com.shop.billing.util.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class NewBillViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val customerRepository: CustomerRepository,
    private val invoiceRepository: InvoiceRepository,
    private val syncEngine: SyncEngine,
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
    private val _allItemsByBarcode = MutableStateFlow<Map<String, ShopItem>>(emptyMap())
    private fun buildBarcodeMap(items: List<ShopItem>): Map<String, ShopItem> =
        items.filter { it.barcode.isNotBlank() }.associate { it.barcode.trim() to it }

    private var currentShopCode = ""

    init {
        val prefs = runBlocking(Dispatchers.IO) { context.dataStore.data.first() }
        currentShopCode = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: ""

        if (currentShopCode.isNotBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                val items = productRepository.getAll(currentShopCode).map { it.toShopItem() }
                _allItems.value = items
                _allItemsByBarcode.value = buildBarcodeMap(items)
            }
            viewModelScope.launch(Dispatchers.IO) {
                _allCustomers.value = customerRepository.getAll(currentShopCode).map { it.toCustomer() }
            }
            viewModelScope.launch {
                syncEngine.pushPending(currentShopCode)
            }
            viewModelScope.launch {
                productRepository.observeAll(currentShopCode).collect { entities ->
                    val items = entities.map { it.toShopItem() }
                    _allItems.value = items
                    _allItemsByBarcode.value = buildBarcodeMap(items)
                }
            }
            viewModelScope.launch {
                customerRepository.observeAll(currentShopCode).collect { entities ->
                    _allCustomers.value = entities.map { it.toCustomer() }
                }
            }
        }
    }

    val categories: StateFlow<List<String>> = _allItems
        .map { items -> items.map { it.category }.distinct().filter { it.isNotBlank() } }
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

    val totalAmount: StateFlow<Double> = _cartItems
        .map { items -> items.sumOf { it.subtotal } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: String?) {
        _selectedCategory.value = category
    }

    fun addToCart(item: ShopItem) {
        val existing = _cartItems.value.find { it.itemId == item.id }
        if (existing != null) {
            _cartItems.value = _cartItems.value.map {
                if (it.itemId == item.id) it.copy(quantity = it.quantity + 1) else it
            }
        } else {
            _cartItems.value = _cartItems.value + CartItem(
                itemId = item.id,
                itemName = item.name,
                quantity = 1,
                unitPrice = item.sellingPrice,
                productId = item.id
            )
        }
    }

    fun getKnownBarcodes(): Set<String> {
        val cur = _allItemsByBarcode.value
        if (cur.isNotEmpty()) return cur.keys
        return runBlocking(Dispatchers.IO) {
            val allItems = productRepository.getAll(currentShopCode).map { it.toShopItem() }
            _allItems.value = allItems
            val byBarcode = buildBarcodeMap(allItems)
            _allItemsByBarcode.value = byBarcode
            byBarcode.keys
        }
    }

    fun onBarcodeResults(items: Map<String, Int>) {
        val barcodeMap = _allItemsByBarcode.value
        var found = 0
        val dbLookups = mutableListOf<Pair<String, Int>>()
        for ((barcode, qty) in items) {
            try {
                val shopItem = barcodeMap[barcode.trim()]
                if (shopItem != null) {
                    addToCartInternal(shopItem, qty)
                    found++
                } else {
                    dbLookups.add(barcode.trim() to qty)
                }
            } catch (e: Exception) {
                Log.e("NewBillVM", "Error processing barcode $barcode", e)
                dbLookups.add(barcode.trim() to qty)
            }
        }
        if (found > 0) Log.d("NewBillVM", "Added $found barcoded items from memory")
        if (dbLookups.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                var dbFound = 0
                val stillUnknown = mutableListOf<String>()
                for ((barcode, qty) in dbLookups) {
                    try {
                        var entity = productRepository.getByBarcode(barcode, currentShopCode)
                        if (entity == null) {
                            entity = productRepository.getByBarcodeAnyShop(barcode)
                        }
                        if (entity == null) {
                            entity = productRepository.getByBarcodeTrimmed(barcode)
                        }
                        if (entity != null) {
                            val shopItem = entity.toShopItem()
                            _allItemsByBarcode.value = _allItemsByBarcode.value + (barcode to shopItem)
                            withContext(Dispatchers.Main) { addToCartInternal(shopItem, qty) }
                            dbFound++
                        } else {
                            stillUnknown.add(barcode)
                        }
                    } catch (e: Exception) {
                        Log.e("NewBillVM", "DB lookup error for $barcode", e)
                        stillUnknown.add(barcode)
                    }
                }
                if (dbFound > 0) Log.d("NewBillVM", "Added $dbFound barcoded items from DB fallback")
                if (stillUnknown.isNotEmpty()) {
                    val msg = if (stillUnknown.size == 1)
                        "Unknown barcode: ${stillUnknown[0]} — add it to inventory first"
                    else
                        "${stillUnknown.size} unknown barcode(s): ${stillUnknown.joinToString(", ")} — add them to inventory first"
                    withContext(Dispatchers.Main) { Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
                }
            }
        }
    }

    private fun addToCartInternal(shopItem: ShopItem, qty: Int) {
        val existing = _cartItems.value.find { it.itemId == shopItem.id }
        if (existing != null) {
            _cartItems.value = _cartItems.value.map {
                if (it.itemId == shopItem.id) it.copy(quantity = it.quantity + qty) else it
            }
        } else {
            _cartItems.value = _cartItems.value + CartItem(
                itemId = shopItem.id,
                itemName = shopItem.name,
                quantity = qty,
                unitPrice = shopItem.sellingPrice,
                productId = shopItem.id
            )
        }
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

    fun selectCustomer() {
        _customerSearchQuery.value = ""
        _filteredCustomers.value = emptyList()
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
                        subtotal = it.subtotal,
                        productId = it.productId
                    )
                }

                invoiceRepository.create(bill, billItems, currentShopCode)
                // Decrease stock for each item in the cart
                for (cartItem in cart) {
                    if (cartItem.productId.isNotBlank()) {
                        productRepository.decreaseStock(cartItem.productId, cartItem.quantity)
                    }
                }
                // Update customer stats in Room so pushCustomers() sends accurate data
                val existingCustomer = customerRepository.getByMobile(customerMobile)
                if (existingCustomer != null) {
                    val newPending = if (_paymentStatus.value == "credit")
                        existingCustomer.pendingAmount + total else existingCustomer.pendingAmount
                    customerRepository.updateStats(
                        mobile = customerMobile,
                        totalBills = existingCustomer.totalBills + 1,
                        totalSpent = existingCustomer.totalSpent + total,
                        pendingAmount = newPending,
                        creditAmount = existingCustomer.creditAmount
                    )
                }
                withContext(NonCancellable) {
                    syncEngine.pushPending(currentShopCode)
                }

                Log.d("NewBillVM", "Bill created in Room with id=$billId")
                onResult(billId)
                clearCart()
            } catch (e: Exception) {
                Log.e("NewBillVM", "Error generating bill", e)
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }}
