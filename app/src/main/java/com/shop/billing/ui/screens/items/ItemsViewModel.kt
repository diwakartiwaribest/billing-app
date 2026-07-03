package com.shop.billing.ui.screens.items

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shop.billing.data.local.entity.ProductEntity
import com.shop.billing.data.model.ShopItem
import com.shop.billing.data.repository.ProductRepository
import com.shop.billing.data.sync.SyncEngine
import com.shop.billing.util.Constants
import com.shop.billing.util.dataStore
import android.util.Log
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import javax.inject.Inject

@HiltViewModel
class ItemsViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val syncEngine: SyncEngine,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    private val _isOwner = MutableStateFlow(false)
    val isOwner: StateFlow<Boolean> = _isOwner
    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin

    private val _customCategories = MutableStateFlow<List<String>>(emptyList())
    val customCategories: StateFlow<List<String>> = _customCategories

    private val _allItems = MutableStateFlow<List<ShopItem>>(emptyList())
    private val _stockFilter = MutableStateFlow("")

    val categories: StateFlow<List<String>> = combine(
        _allItems,
        _customCategories
    ) { allItems, customCats ->
        val itemCats = allItems.map { it.category }
        (itemCats + customCats).distinct().filter { it.isNotBlank() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<String>> = categories

    val items: StateFlow<List<ShopItem>> = combine(
        _searchQuery,
        _selectedCategory,
        _stockFilter,
        _allItems
    ) { query, category, stockFilter, allItems ->
        allItems.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.name.contains(query, ignoreCase = true) ||
                    item.category.contains(query, ignoreCase = true)
            val matchesCategory = category == null || item.category == category
            val matchesStock = when (stockFilter) {
                "low" -> item.stockQuantity > 0 && item.stockQuantity <= item.lowStockThreshold
                "out" -> item.stockQuantity == 0
                else -> true
            }
            matchesQuery && matchesCategory && matchesStock
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val filterLabel: String get() = when (_stockFilter.value) {
        "low" -> "Low Stock Items"
        "out" -> "Out of Stock Items"
        else -> "Items"
    }

    private var currentShopCode = ""

    init {
        loadCustomCategories()
        viewModelScope.launch {
            try {
                val prefs = context.dataStore.data.first()
                val role = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ROLE)] ?: "member"
                _isOwner.value = role == "owner"
                _isAdmin.value = role == "admin"
                currentShopCode = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: ""
            } catch (_: Exception) {}
        }
        viewModelScope.launch {
            val prefs = context.dataStore.data.first()
            val code = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: ""
            if (code.isNotBlank()) {
                _allItems.value = productRepository.getAll(code).map { it.toShopItem() }
            }
        }
        viewModelScope.launch {
            val code = context.dataStore.data.first()[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: ""
            if (code.isNotBlank()) {
                syncEngine.pushPending(code)
            }
        }
        viewModelScope.launch {
            val prefs = context.dataStore.data.first()
            val code = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: ""
            if (code.isNotBlank()) {
                productRepository.observeAll(code).collect { entities ->
                    _allItems.value = entities.map { it.toShopItem() }
                }
            }
        }
    }

    private fun triggerSync() {
        viewModelScope.launch {
            val code = context.dataStore.data.first()[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: ""
            if (code.isNotBlank()) {
                withContext(NonCancellable) {
                    syncEngine.pushPending(code)
                }
            }
        }
    }

    private fun loadCustomCategories() {
        viewModelScope.launch {
            try {
                val prefs = context.dataStore.data.first()
                val json = prefs[stringPreferencesKey("custom_categories")] ?: "[]"
                val arr = JSONArray(json)
                val list = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    list.add(arr.getString(i))
                }
                _customCategories.value = list
            } catch (_: Exception) {
                _customCategories.value = emptyList()
            }
        }
    }

    fun addCategory(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        val current = _customCategories.value.toMutableList()
        if (!current.contains(trimmed)) {
            current.add(trimmed)
            _customCategories.value = current
            saveCustomCategories(current)
        }
    }

    fun deleteCategory(category: String) {
        viewModelScope.launch {
            val shopCode = if (currentShopCode.isNotBlank()) currentShopCode else {
                context.dataStore.data.first()[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: ""
            }
            if (shopCode.isNotBlank()) {
                productRepository.getAll(shopCode).filter { it.category == category }.forEach {
                    productRepository.softDelete(it.id)
                }
            }
            val custom = _customCategories.value.toMutableList()
            if (custom.remove(category)) {
                _customCategories.value = custom
                saveCustomCategories(custom)
            }
            triggerSync()
        }
    }

    private fun saveCustomCategories(list: List<String>) {
        viewModelScope.launch {
            val arr = JSONArray()
            list.forEach { arr.put(it) }
            context.dataStore.edit { prefs ->
                prefs[stringPreferencesKey("custom_categories")] = arr.toString()
            }
        }
    }

    fun setStockFilter(filter: String) {
        _stockFilter.value = filter
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: String?) {
        _selectedCategory.value = category
    }

    fun getItemByBarcode(barcode: String, onResult: (ShopItem?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val shopCode = if (currentShopCode.isNotBlank()) currentShopCode else {
                context.dataStore.data.first()[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: ""
            }
            val existing = productRepository.getByBarcode(barcode.trim(), shopCode)
            withContext(Dispatchers.Main) { onResult(existing?.toShopItem()) }
        }
    }

    fun addItem(name: String, sellingPrice: Double, buyingPrice: Double = 0.0, category: String, stockQuantity: Int = 0, lowStockThreshold: Int = 10, barcode: String = "") {
        viewModelScope.launch {
            val item = ShopItem(name = name, sellingPrice = sellingPrice, buyingPrice = buyingPrice, category = category, barcode = barcode, stockQuantity = stockQuantity, lowStockThreshold = lowStockThreshold)
            productRepository.create(item, currentShopCode)
            triggerSync()
        }
    }

    fun updateItem(item: ShopItem) {
        viewModelScope.launch {
            val existing = productRepository.getById(item.id)
            if (existing != null) {
                productRepository.update(
                    existing.copy(name = item.name, sellingPrice = item.sellingPrice, buyingPrice = item.buyingPrice, category = item.category, stockQuantity = item.stockQuantity, lowStockThreshold = item.lowStockThreshold)
                )
                triggerSync()
            }
        }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            try {
                val shopCode = if (currentShopCode.isNotBlank()) currentShopCode else {
                    context.dataStore.data.first()[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: ""
                }
                if (shopCode.isNotBlank()) {
                    productRepository.softDelete(id)
                    triggerSync()
                }
            } catch (e: Exception) {
                Log.e("ItemsVM", "deleteItem failed", e)
            }
        }
    }

    fun deleteItems(ids: List<String>) {
        viewModelScope.launch {
            try {
                val shopCode = if (currentShopCode.isNotBlank()) currentShopCode else {
                    context.dataStore.data.first()[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: ""
                }
                if (shopCode.isNotBlank()) {
                    for (id in ids) {
                        productRepository.softDelete(id)
                    }
                    triggerSync()
                }
            } catch (e: Exception) {
                Log.e("ItemsVM", "deleteItems failed", e)
            }
        }
    }
}