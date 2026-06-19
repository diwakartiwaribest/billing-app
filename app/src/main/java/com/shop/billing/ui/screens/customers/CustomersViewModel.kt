package com.shop.billing.ui.screens.customers

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shop.billing.data.local.entity.CustomerEntity
import com.shop.billing.data.model.Customer
import com.shop.billing.data.repository.CustomerRepository
import com.shop.billing.data.sync.SyncEngine
import com.shop.billing.util.Constants
import com.shop.billing.util.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CustomersViewModel @Inject constructor(
    private val customerRepository: CustomerRepository,
    private val syncEngine: SyncEngine,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private var currentShopCode = ""

    private val _userRole = MutableStateFlow("member")
    val userRole: StateFlow<String> = _userRole

    private val _customers = MutableStateFlow<List<Customer>>(emptyList())
    val customers: StateFlow<List<Customer>> = _customers

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val filteredCustomers: StateFlow<List<Customer>> = MutableStateFlow(emptyList())
    private val _filtered = filteredCustomers as MutableStateFlow

    init {
        viewModelScope.launch {
            val prefs = context.dataStore.data.first()
            currentShopCode = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: ""
            _userRole.value = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ROLE)] ?: "member"
            if (currentShopCode.isNotBlank()) {
                launch {
                    customerRepository.observeAll(currentShopCode).collect { entities ->
                        _customers.value = entities.map { it.toCustomer() }
                        applyFilter()
                    }
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilter()
    }

    private fun applyFilter() {
        val query = _searchQuery.value.trim().lowercase()
        _filtered.value = if (query.isBlank()) _customers.value
        else _customers.value.filter {
            it.name.lowercase().contains(query) || it.mobile.contains(query)
        }
    }

    fun addCustomer(name: String, mobile: String) {
        viewModelScope.launch {
            val shopCode = currentShopCode
            if (shopCode.isBlank()) return@launch
            val customer = Customer(name = name, mobile = mobile)
            customerRepository.create(customer, shopCode)
            withContext(Dispatchers.IO) { syncEngine.pushPending(shopCode) }
        }
    }

    fun updateCustomer(originalMobile: String, name: String, newMobile: String) {
        viewModelScope.launch {
            val shopCode = currentShopCode
            if (shopCode.isBlank()) return@launch
            val entity = CustomerEntity(
                mobile = newMobile,
                name = name,
                shopCode = shopCode
            )
            customerRepository.update(entity)
            if (originalMobile != newMobile) {
                customerRepository.softDelete(originalMobile, shopCode)
            }
            withContext(Dispatchers.IO) { syncEngine.pushPending(shopCode) }
        }
    }

    fun deleteCustomer(mobile: String) {
        if (_userRole.value != "owner" && _userRole.value != "admin") return
        viewModelScope.launch {
            val shopCode = currentShopCode
            if (shopCode.isBlank()) return@launch
            customerRepository.softDelete(mobile, shopCode)
            withContext(Dispatchers.IO) { syncEngine.pushPending(shopCode) }
        }
    }
}
