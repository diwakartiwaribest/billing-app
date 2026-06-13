package com.shop.billing.ui.screens.ledger

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shop.billing.data.model.Bill
import com.shop.billing.data.model.Customer
import com.shop.billing.data.model.CustomerPayment
import com.shop.billing.data.remote.SupabaseClient
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
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CustomerLedgerViewModel @Inject constructor(
    private val supabaseClient: SupabaseClient,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _pendingDeletedPayment = MutableStateFlow<CustomerPayment?>(null)
    val pendingDeletedPayment: StateFlow<CustomerPayment?> = _pendingDeletedPayment

    private var _pendingDeletedPaymentMobile = ""

    private val _allCustomers = MutableStateFlow<List<Customer>>(emptyList())
    private val _allPayments = MutableStateFlow<List<CustomerPayment>>(emptyList())
    private val _allBills = MutableStateFlow<List<Bill>>(emptyList())

    val totalPending: StateFlow<Double> = _allCustomers
        .map { list -> list.sumOf { it.pendingAmount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalPaid: StateFlow<Double> = _allPayments
        .map { list -> list.sumOf { it.amount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val customers: StateFlow<List<Customer>> = combine(_allCustomers, _searchQuery) { all, query ->
        val q = query.trim().lowercase()
        if (q.isBlank()) all
        else all.filter { it.name.lowercase().contains(q) || it.mobile.contains(q) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customerCount: StateFlow<Int> = customers
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val paymentsCache = mutableMapOf<String, StateFlow<List<CustomerPayment>>>()
    private val billsCache = mutableMapOf<String, StateFlow<List<Bill>>>()
    private val totalPaidCache = mutableMapOf<String, StateFlow<Double>>()

    init {
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val prefs = context.dataStore.data.first()
                val url = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_URL)] ?: Constants.HARDCODED_SUPABASE_URL
                val key = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_KEY)] ?: Constants.HARDCODED_SUPABASE_KEY
                val code = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: ""
                if (url.isNotBlank() && key.isNotBlank() && code.isNotBlank()) {
                    val customers = supabaseClient.pullCustomers(url, key, code)
                    val payments = supabaseClient.pullCustomerPayments(url, key, code)
                    val (bills, _) = supabaseClient.pullBills(url, key, code)
                    _allCustomers.value = customers
                    _allPayments.value = payments
                    _allBills.value = bills
                }
            } catch (e: Exception) {
                Log.e("LedgerVM", "refreshData failed", e)
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun addPayment(customerMobile: String, amount: Double, note: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            val payment = CustomerPayment(
                customerMobile = customerMobile,
                amount = amount,
                note = note
            )
            _allPayments.value = _allPayments.value + payment

            val idx = _allCustomers.value.indexOfFirst { it.mobile == customerMobile }
            if (idx >= 0) {
                val updated = _allCustomers.value.toMutableList()
                val c = updated[idx]
                updated[idx] = c.copy(pendingAmount = c.pendingAmount - amount)
                _allCustomers.value = updated
            }

            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val prefs = context.dataStore.data.first()
                    val url = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_URL)] ?: Constants.HARDCODED_SUPABASE_URL
                    val key = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_KEY)] ?: Constants.HARDCODED_SUPABASE_KEY
                    val code = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: ""
                    if (url.isNotBlank() && key.isNotBlank() && code.isNotBlank()) {
                        supabaseClient.pushAllCustomerPayments(url, key, code, listOf(payment))
                        val c = _allCustomers.value.find { it.mobile == customerMobile }
                        if (c != null) {
                            supabaseClient.updateCustomerStats(url, key, code, customerMobile, c.totalBills, c.totalSpent, c.pendingAmount)
                        }
                    }
                } catch (_: Exception) {}
            }
            onComplete()
        }
    }

    fun deletePayment(payment: CustomerPayment, customerMobile: String) {
        _pendingDeletedPayment.value = payment
        _pendingDeletedPaymentMobile = customerMobile
    }

    fun undoDeletePayment() {
        _pendingDeletedPayment.value = null
        _pendingDeletedPaymentMobile = ""
    }

    fun confirmDeletePayment() {
        val payment = _pendingDeletedPayment.value ?: return
        val mobile = _pendingDeletedPaymentMobile
        viewModelScope.launch {
            _allPayments.value = _allPayments.value.filter {
                it.uuid != payment.uuid || it.customerMobile != payment.customerMobile
            }

            val idx = _allCustomers.value.indexOfFirst { it.mobile == mobile }
            if (idx >= 0) {
                val updated = _allCustomers.value.toMutableList()
                val c = updated[idx]
                updated[idx] = c.copy(pendingAmount = c.pendingAmount + payment.amount)
                _allCustomers.value = updated
            }

            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val prefs = context.dataStore.data.first()
                    val url = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_URL)] ?: Constants.HARDCODED_SUPABASE_URL
                    val key = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_KEY)] ?: Constants.HARDCODED_SUPABASE_KEY
                    val code = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: ""
                    if (url.isNotBlank() && key.isNotBlank() && code.isNotBlank()) {
                        supabaseClient.deleteCustomerPaymentByUuid(url, key, payment.uuid)
                        val c = _allCustomers.value.find { it.mobile == mobile }
                        if (c != null) {
                            supabaseClient.updateCustomerStats(url, key, code, mobile, c.totalBills, c.totalSpent, c.pendingAmount)
                        }
                    }
                } catch (_: Exception) {}
            }
        }
        _pendingDeletedPayment.value = null
        _pendingDeletedPaymentMobile = ""
    }

    fun clearPaymentHistory(customerMobile: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val prefs = context.dataStore.data.first()
                val url = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_URL)] ?: Constants.HARDCODED_SUPABASE_URL
                val key = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_KEY)] ?: Constants.HARDCODED_SUPABASE_KEY
                val code = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: ""
                if (url.isNotBlank() && key.isNotBlank() && code.isNotBlank()) {
                    supabaseClient.deleteAllCustomerPaymentsForCustomer(url, key, code, customerMobile)
                    supabaseClient.deleteAllBillsForCustomer(url, key, code, customerMobile)
                    supabaseClient.updateCustomerStats(url, key, code, customerMobile, 0, 0.0, 0.0)
                }
                _allPayments.value = _allPayments.value.filter { it.customerMobile != customerMobile }
                _allBills.value = _allBills.value.filter { it.customerMobile != customerMobile }
                val idx = _allCustomers.value.indexOfFirst { it.mobile == customerMobile }
                if (idx >= 0) {
                    val updated = _allCustomers.value.toMutableList()
                    updated[idx] = updated[idx].copy(totalBills = 0, totalSpent = 0.0, pendingAmount = 0.0)
                    _allCustomers.value = updated
                }
            } catch (e: Exception) {
                Log.e("LedgerVM", "clearPaymentHistory failed", e)
            }
        }
    }

    fun getPaymentsForCustomer(mobile: String): StateFlow<List<CustomerPayment>> {
        return paymentsCache.getOrPut(mobile) {
            _allPayments.map { it.filter { p -> p.customerMobile == mobile } }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        }
    }

    fun getBillsForCustomer(mobile: String): StateFlow<List<Bill>> {
        return billsCache.getOrPut(mobile) {
            _allBills.map { it.filter { b -> b.customerMobile == mobile } }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        }
    }

    fun getTotalPaidForCustomer(mobile: String): StateFlow<Double> {
        return totalPaidCache.getOrPut(mobile) {
            _allPayments.map {
                it.filter { p -> p.customerMobile == mobile }.sumOf { p -> p.amount }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
        }
    }

    fun syncPayments() {
        refreshData()
    }
}
