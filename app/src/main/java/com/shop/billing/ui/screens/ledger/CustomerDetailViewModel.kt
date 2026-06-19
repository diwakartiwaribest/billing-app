package com.shop.billing.ui.screens.ledger

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shop.billing.data.model.Bill
import com.shop.billing.data.model.Customer
import com.shop.billing.data.model.CustomerPayment
import com.shop.billing.data.repository.CustomerPaymentRepository
import com.shop.billing.data.repository.CustomerRepository
import com.shop.billing.data.repository.InvoiceRepository
import com.shop.billing.data.sync.SyncEngine
import com.shop.billing.util.Constants
import com.shop.billing.util.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CustomerDetailViewModel @Inject constructor(
    private val customerRepository: CustomerRepository,
    private val invoiceRepository: InvoiceRepository,
    private val paymentRepository: CustomerPaymentRepository,
    private val syncEngine: SyncEngine,
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

    private var currentShopCode = ""

    init {
        viewModelScope.launch {
            if (customerMobile.isBlank()) return@launch
            val prefs = context.dataStore.data.first()
            currentShopCode = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: ""

            if (currentShopCode.isNotBlank()) {
                // Observe customer data reactively from Room
                launch {
                    customerRepository.observeByMobile(customerMobile).collect { entity ->
                        _customer.value = entity?.toCustomer()
                    }
                }
                // Observe invoices for this customer
                launch {
                    invoiceRepository.observeByCustomerMobile(customerMobile, currentShopCode).collect { entities ->
                        _bills.value = entities.map { it.toBill() }
                    }
                }
                // Observe payments for this customer
                launch {
                    paymentRepository.observeByCustomerMobile(customerMobile, currentShopCode).collect { entities ->
                        val filtered = entities.map { it.toCustomerPayment() }
                        _payments.value = filtered
                        _totalPaid.value = filtered.sumOf { it.amount }
                    }
                }
                // Sync in background
                launch {
                    syncEngine.pushPending(currentShopCode)
                }
            }
        }
    }

    fun addPayment(amount: Double, note: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            val payment = CustomerPayment(
                customerMobile = customerMobile,
                amount = amount,
                note = note
            )
            paymentRepository.create(payment, currentShopCode)
            _totalPaid.value = _totalPaid.value + amount
            // Update customer stats in Room so pushCustomers() sends accurate data
            val existing = customerRepository.getByMobile(customerMobile)
            if (existing != null) {
                val newPending = (existing.pendingAmount - amount).coerceAtLeast(0.0)
                val newCredit = existing.creditAmount + (amount - existing.pendingAmount).coerceAtLeast(0.0)
                customerRepository.updateStats(
                    mobile = customerMobile,
                    totalBills = existing.totalBills,
                    totalSpent = existing.totalSpent,
                    pendingAmount = newPending,
                    creditAmount = newCredit
                )
            }
            withContext(NonCancellable) {
                syncEngine.pushPending(currentShopCode)
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
            val shopCode = if (currentShopCode.isNotBlank()) currentShopCode else {
                context.dataStore.data.first()[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: ""
            }
            paymentRepository.softDelete(payment.uuid, shopCode)
            _totalPaid.value = _totalPaid.value - payment.amount
            // Update customer stats in Room
            val existing = customerRepository.getByMobile(customerMobile)
            if (existing != null) {
                val deductFromCredit = payment.amount.coerceAtMost(existing.creditAmount)
                customerRepository.updateStats(
                    mobile = customerMobile,
                    totalBills = existing.totalBills,
                    totalSpent = existing.totalSpent,
                    pendingAmount = existing.pendingAmount + (payment.amount - deductFromCredit),
                    creditAmount = existing.creditAmount - deductFromCredit
                )
            }
            withContext(NonCancellable) {
                syncEngine.pushPending(currentShopCode)
            }
        }
        _pendingDeletedPayment.value = null
    }

    fun clearPaymentHistory() {
        viewModelScope.launch {
            try {
                val shopCode = if (currentShopCode.isNotBlank()) currentShopCode else {
                    context.dataStore.data.first()[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: ""
                }
                val payments = paymentRepository.getByCustomerMobile(customerMobile)
                for (p in payments) {
                    paymentRepository.softDelete(p.uuid, shopCode)
                }
                val invoices = invoiceRepository.getByCustomerMobile(customerMobile)
                for (inv in invoices) {
                    invoiceRepository.softDelete(inv.id, shopCode)
                }
                // Reset customer stats in Room
                val existing = customerRepository.getByMobile(customerMobile)
                if (existing != null) {
                    customerRepository.updateStats(
                        mobile = customerMobile,
                        totalBills = 0,
                        totalSpent = 0.0,
                        pendingAmount = 0.0,
                        creditAmount = 0.0
                    )
                }
            withContext(NonCancellable) {
                syncEngine.pushPending(currentShopCode)
            }
                // Force UI refresh after clearing
                refresh()
            } catch (e: Exception) {
                Log.e("DetailVM", "clearPaymentHistory failed", e)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            syncEngine.pushPending(currentShopCode)
        }
    }
}
