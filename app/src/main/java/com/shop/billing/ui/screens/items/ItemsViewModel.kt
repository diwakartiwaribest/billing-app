package com.shop.billing.ui.screens.items

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shop.billing.data.model.ShopItem
import com.shop.billing.data.remote.SupabaseClient
import com.shop.billing.data.repository.ShopItemRepository
import com.shop.billing.util.Constants
import com.shop.billing.util.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import javax.inject.Inject

@HiltViewModel
class ItemsViewModel @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val shopItemRepository: ShopItemRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    private val _pendingDeletedItem = MutableStateFlow<ShopItem?>(null)
    val pendingDeletedItem: StateFlow<ShopItem?> = _pendingDeletedItem

    private val _isOwner = MutableStateFlow(false)
    val isOwner: StateFlow<Boolean> = _isOwner

    private val _customCategories = MutableStateFlow<List<String>>(emptyList())
    val customCategories: StateFlow<List<String>> = _customCategories

    private val _allItems = MutableStateFlow<List<ShopItem>>(emptyList())

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

    init {
        loadCustomCategories()
        viewModelScope.launch {
            try {
                val prefs = context.dataStore.data.first()
                _isOwner.value = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ROLE)] == "owner"
            } catch (_: Exception) {}
        }
        loadFromRoom()
        pullFromSupabase()
    }

    private fun loadFromRoom() {
        viewModelScope.launch {
            shopItemRepository.getAllItems().collect { items ->
                _allItems.value = items
            }
        }
    }

    private fun pullFromSupabase() {
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
            try {
                val prefs = context.dataStore.data.first()
                val url = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_URL)] ?: return@launch
                val key = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_KEY)] ?: return@launch
                val code = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: return@launch
                supabaseClient.deleteShopItemsByCategory(url, key, code, category)
                shopItemRepository.deleteAll()
                pullFromSupabase()
            } catch (_: Exception) {}
            val custom = _customCategories.value.toMutableList()
            if (custom.remove(category)) {
                _customCategories.value = custom
                saveCustomCategories(custom)
            }
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

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: String?) {
        _selectedCategory.value = category
    }

    fun addItem(name: String, price: Double, category: String) {
        viewModelScope.launch {
            val item = ShopItem(name = name, price = price, category = category)
            shopItemRepository.insertItem(item)
            val prefs = context.dataStore.data.first()
            val url = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_URL)] ?: Constants.HARDCODED_SUPABASE_URL
            val key = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_KEY)] ?: Constants.HARDCODED_SUPABASE_KEY
            val code = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: return@launch
            supabaseClient.pushShopItem(url, key, code, item)
        }
    }

    fun updateItem(item: ShopItem) {
        viewModelScope.launch {
            shopItemRepository.updateItem(item.id, item.name, item.price, item.category)
            val prefs = context.dataStore.data.first()
            val url = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_URL)] ?: Constants.HARDCODED_SUPABASE_URL
            val key = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_KEY)] ?: Constants.HARDCODED_SUPABASE_KEY
            val code = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: return@launch
            supabaseClient.updateShopItem(url, key, code, item.id, item.name, item.price, item.category)
        }
    }

    fun deleteItem(item: ShopItem) {
        _pendingDeletedItem.value = item
    }

    fun undoDeleteItem() {
        _pendingDeletedItem.value = null
    }

    fun confirmDeleteItem() {
        val item = _pendingDeletedItem.value ?: return
        viewModelScope.launch {
            try {
                shopItemRepository.deleteItem(item.id)
                val prefs = context.dataStore.data.first()
                val url = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_URL)] ?: return@launch
                val key = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_KEY)] ?: return@launch
                val code = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: return@launch
                supabaseClient.deleteShopItem(url, key, code, item.id)
            } catch (_: Exception) {}
        }
        _pendingDeletedItem.value = null
    }
}
