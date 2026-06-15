package com.shop.billing.ui.screens.ledger

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shop.billing.data.AppDataCache
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CustomerDetailViewModel @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val dataCache: AppDataCache,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val customerMobile: String = savedStateHandle.get<String>("mobile") ?: ""

    private val _customer = MutableStateFlow<Customer?>(null)
    val customer: StateFlow<Customer?> = _customer

    private val _bills = MutableStateFlow<List<Bill>>(emptyList())
    val bills: StateFlow<List<Bill>> = _bills

    private val _payments = MutableStateFlow<List<CustomerPayment>>(emptyList())
    val payments: StateFlow<List<CustomerPayment>> = _payments

    private val _totalPaid = MutableStateFlow(0.0)
    val totalPaid: StateFlow<Double> = _totalPaid

    private val _pendingDeletedPayment = MutableStateFlow<CustomerPayment?>(null)
    val pendingDeletedPayment: StateFlow<CustomerPayment?> = _pendingDeletedPayment

    init {
        loadData()
    }

    private fun loadData() {
        if (customerMobile.isBlank()) return
        // Use cached data instantly if available
        if (dataCache.customersLoaded && dataCache.billsLoaded && dataCache.paymentsLoaded) {
            _customer.value = dataCache.customers.find { it.mobile == customerMobile }
            _bills.value = dataCache.bills.filter { it.customerMobile == customerMobile }
            _payments.value = dataCache.payments.filter { it.customerMobile == customerMobile }
            _totalPaid.value = _payments.value.sumOf { it.amount }
        }
        // Always refresh from network
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val prefs = context.dataStore.data.first()
                val url = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_URL)] ?: Constants.HARDCODED_SUPABASE_URL
                val key = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_KEY)] ?: Constants.HARDCODED_SUPABASE_KEY
                val code = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: ""
                if (url.isNotBlank() && key.isNotBlank() && code.isNotBlank()) {
                    val customers = supabaseClient.pullCustomers(url, key, code)
                    _customer.value = customers.find { it.mobile == customerMobile }

                    val (bills, _) = supabaseClient.pullBills(url, key, code)
                    val customerBills = bills.filter { it.customerMobile == customerMobile }
                    _bills.value = customerBills

                    val payments = supabaseClient.pullCustomerPayments(url, key, code)
                    val customerPayments = payments.filter { it.customerMobile == customerMobile }
                    _payments.value = customerPayments
                    _totalPaid.value = customerPayments.sumOf { it.amount }

                    // Update cache (only what we fetched)
                    dataCache.setCustomers(customers)
                    dataCache.setPayments(payments)
                }
            } catch (e: Exception) {
                Log.e("DetailVM", "loadData failed", e)
            }
        }
    }

    fun refresh() {
        loadData()
    }

    fun addPayment(amount: Double, note: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            val payment = CustomerPayment(
                customerMobile = customerMobile,
                amount = amount,
                note = note
            )
            _payments.value = _payments.value + payment
            _totalPaid.value = _totalPaid.value + amount

            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val prefs = context.dataStore.data.first()
                    val url = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_URL)] ?: Constants.HARDCODED_SUPABASE_URL
                    val key = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_KEY)] ?: Constants.HARDCODED_SUPABASE_KEY
                    val code = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: ""
                    if (url.isNotBlank() && key.isNotBlank() && code.isNotBlank()) {
                        supabaseClient.pushAllCustomerPayments(url, key, code, listOf(payment))
                        val c = _customer.value
                        if (c != null) {
                            supabaseClient.updateCustomerStats(
                                url, key, code, customerMobile,
                                c.totalBills, c.totalSpent, c.pendingAmount - amount
                            )
                        }
                    }
                } catch (_: Exception) {}
            }
            onComplete()
        }
    }

    fun deletePayment(payment: CustomerPayment) {
        _pendingDeletedPayment.value = payment
    }

    fun undoDeletePayment() {
        _pendingDeletedPayment.value = null
    }

    fun confirmDeletePayment() {
        val payment = _pendingDeletedPayment.value ?: return
        viewModelScope.launch {
            _payments.value = _payments.value.filter { it.uuid != payment.uuid }
            _totalPaid.value = _totalPaid.value - payment.amount

            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val prefs = context.dataStore.data.first()
                    val url = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_URL)] ?: Constants.HARDCODED_SUPABASE_URL
                    val key = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_KEY)] ?: Constants.HARDCODED_SUPABASE_KEY
                    val code = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: ""
                    if (url.isNotBlank() && key.isNotBlank() && code.isNotBlank()) {
                        supabaseClient.deleteCustomerPaymentByUuid(url, key, payment.uuid)
                        val c = _customer.value
                        if (c != null) {
                            supabaseClient.updateCustomerStats(
                                url, key, code, customerMobile,
                                c.totalBills, c.totalSpent, c.pendingAmount + payment.amount
                            )
                        }
                    }
                } catch (_: Exception) {}
            }
        }
        _pendingDeletedPayment.value = null
    }

    fun clearPaymentHistory() {
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
                _payments.value = emptyList()
                _bills.value = emptyList()
                _totalPaid.value = 0.0
                _customer.value = _customer.value?.copy(
                    totalBills = 0, totalSpent = 0.0, pendingAmount = 0.0
                )
            } catch (e: Exception) {
                Log.e("DetailVM", "clearPaymentHistory failed", e)
            }
        }
    }
}
