package com.shop.billing.ui.screens.ledger

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shop.billing.data.model.Bill
import com.shop.billing.data.model.BillItem
import com.shop.billing.data.model.Customer
import com.shop.billing.data.model.CustomerPayment
import com.shop.billing.data.remote.SupabaseClient
import com.shop.billing.data.remote.SupabaseRealtimeClient
import com.shop.billing.util.Constants
import com.shop.billing.util.PdfGenerator
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
import java.io.File
import javax.inject.Inject

@HiltViewModel
class CustomerLedgerViewModel @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val realtimeClient: SupabaseRealtimeClient,
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
    private val _allBillItems = MutableStateFlow<List<BillItem>>(emptyList())

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
        viewModelScope.launch {
            try {
                val prefs = context.dataStore.data.first()
                val url = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_URL)] ?: Constants.HARDCODED_SUPABASE_URL
                val key = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_KEY)] ?: Constants.HARDCODED_SUPABASE_KEY
                if (url.isNotBlank() && key.isNotBlank()) {
                    realtimeClient.connect(url, key)
                }
            } catch (_: Exception) {}
        }
        viewModelScope.launch {
            realtimeClient.events.collect { change ->
                if (change.table == "customers" || change.table == "customer_payments" || change.table == "bills") {
                    refreshData()
                }
            }
        }
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
                    val (bills, billItems) = supabaseClient.pullBills(url, key, code)

                    val invoiceBills = bills.filter { it.paymentStatus == "invoice" }
                    if (invoiceBills.isNotEmpty()) {
                        invoiceBills.forEach { inv ->
                            try {
                                supabaseClient.deleteBill(url, key, code, inv.id)
                            } catch (_: Exception) {}
                        }
                    }

                    val cleanBills = bills.filter { it.paymentStatus != "invoice" }
                    val cleanItems = billItems.filter { item -> cleanBills.any { it.id == item.billId } }
                    val recalculated = customers.map { c ->
                        val customerBills = cleanBills.filter { it.customerMobile == c.mobile }
                        val customerPayments = payments.filter { it.customerMobile == c.mobile }
                        val creditTotal = customerBills.filter { it.paymentStatus == "credit" }.sumOf { it.totalAmount }
                        val totalPaid = customerPayments.sumOf { it.amount }
                        val pending = (creditTotal - totalPaid).coerceAtLeast(0.0)
                        val credit = (totalPaid - creditTotal).coerceAtLeast(0.0)
                        c.copy(pendingAmount = pending, creditAmount = credit)
                    }
                    _allCustomers.value = recalculated
                    _allPayments.value = payments
                    _allBills.value = cleanBills
                    _allBillItems.value = cleanItems
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
        viewModelScope.launch(Dispatchers.IO) {
            val payment = CustomerPayment(
                customerMobile = customerMobile,
                amount = amount,
                note = note
            )
            var savedPayment = payment
            try {
                val prefs = context.dataStore.data.first()
                val url = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_URL)] ?: Constants.HARDCODED_SUPABASE_URL
                val key = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_KEY)] ?: Constants.HARDCODED_SUPABASE_KEY
                val code = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: ""
                if (url.isNotBlank() && key.isNotBlank() && code.isNotBlank()) {
                    val id = supabaseClient.pushCustomerPayment(url, key, code, payment)
                    if (id != null) {
                        savedPayment = payment.copy(id = id)
                    }
                }
            } catch (_: Exception) {}

            _allPayments.value = _allPayments.value + savedPayment

            val idx = _allCustomers.value.indexOfFirst { it.mobile == customerMobile }
            if (idx >= 0) {
                val updated = _allCustomers.value.toMutableList()
                val c = updated[idx]
                val newPending = (c.pendingAmount - amount).coerceAtLeast(0.0)
                val newCredit = c.creditAmount + (amount - c.pendingAmount).coerceAtLeast(0.0)
                updated[idx] = c.copy(pendingAmount = newPending, creditAmount = newCredit)
                _allCustomers.value = updated

                try {
                    val prefs = context.dataStore.data.first()
                    val url = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_URL)] ?: Constants.HARDCODED_SUPABASE_URL
                    val key = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_KEY)] ?: Constants.HARDCODED_SUPABASE_KEY
                    val code = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: ""
                    if (url.isNotBlank() && key.isNotBlank() && code.isNotBlank()) {
                        supabaseClient.updateCustomerStats(url, key, code, customerMobile, c.totalBills, c.totalSpent, newPending, newCredit)
                    }
                } catch (_: Exception) {}
            }

            withContext(Dispatchers.Main) {
                onComplete()
            }
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
                val deductFromCredit = payment.amount.coerceAtMost(c.creditAmount)
                updated[idx] = c.copy(
                    pendingAmount = c.pendingAmount + (payment.amount - deductFromCredit),
                    creditAmount = c.creditAmount - deductFromCredit
                )
                _allCustomers.value = updated
            }

            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val prefs = context.dataStore.data.first()
                    val url = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_URL)] ?: Constants.HARDCODED_SUPABASE_URL
                    val key = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_KEY)] ?: Constants.HARDCODED_SUPABASE_KEY
                    val code = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: ""
                    if (url.isNotBlank() && key.isNotBlank() && code.isNotBlank()) {
                        if (payment.id != null) {
                            supabaseClient.deleteCustomerPayment(url, key, code, payment.id)
                        } else {
                            supabaseClient.deleteCustomerPaymentByMatch(url, key, code, mobile, payment.amount, SupabaseClient.millisToIso(payment.createdAt))
                        }
                        val c = _allCustomers.value.find { it.mobile == mobile }
                        if (c != null) {
                            supabaseClient.updateCustomerStats(url, key, code, mobile, c.totalBills, c.totalSpent, c.pendingAmount, c.creditAmount)
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
                    supabaseClient.updateCustomerStats(url, key, code, customerMobile, 0, 0.0, 0.0, 0.0)
                }
                _allPayments.value = _allPayments.value.filter { it.customerMobile != customerMobile }
                _allBills.value = _allBills.value.filter { it.customerMobile != customerMobile }
                val idx = _allCustomers.value.indexOfFirst { it.mobile == customerMobile }
                if (idx >= 0) {
                    val updated = _allCustomers.value.toMutableList()
                    updated[idx] = updated[idx].copy(totalBills = 0, totalSpent = 0.0, pendingAmount = 0.0, creditAmount = 0.0)
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
            _allBills.map { it.filter { b -> b.customerMobile == mobile && b.paymentStatus != "invoice" } }
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

    fun generateAndSharePendingInvoice(customerMobile: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val prefs = context.dataStore.data.first()
                val url = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_URL)] ?: Constants.HARDCODED_SUPABASE_URL
                val key = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_KEY)] ?: Constants.HARDCODED_SUPABASE_KEY
                val code = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: ""
                val shopName = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_NAME)] ?: Constants.DEFAULT_SHOP_NAME
                val shopAddress = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_ADDRESS)] ?: Constants.DEFAULT_SHOP_ADDRESS
                val shopPhone = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_PHONE)] ?: Constants.DEFAULT_SHOP_PHONE
                val logoBase64 = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_LOGO)]
                val invoiceMessage = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_INVOICE_MESSAGE)] ?: ""

                val customer = _allCustomers.value.find { it.mobile == customerMobile } ?: return@launch
                val creditBills = _allBills.value.filter { it.customerMobile == customerMobile && it.paymentStatus == "credit" }.sortedBy { it.createdAt }
                val payments = _allPayments.value.filter { it.customerMobile == customerMobile }.sortedBy { it.createdAt }
                val totalBills = creditBills.sumOf { it.totalAmount }
                val totalPaid = payments.sumOf { it.amount }
                val creditAmount = (totalPaid - totalBills).coerceAtLeast(0.0)

                val unpaidBills = mutableListOf<Bill>()
                var paidLeft = totalPaid
                var fullyPaidBillsTotal = 0.0
                for (bill in creditBills) {
                    if (paidLeft >= bill.totalAmount) {
                        paidLeft -= bill.totalAmount
                        fullyPaidBillsTotal += bill.totalAmount
                    } else {
                        unpaidBills.add(bill)
                        paidLeft = 0.0
                    }
                }

                val paymentsForPdf = if (unpaidBills.isEmpty()) {
                    emptyList()
                } else {
                    val remainingPayments = mutableListOf<CustomerPayment>()
                    var paidConsumedByHiddenBills = fullyPaidBillsTotal
                    for (payment in payments) {
                        if (paidConsumedByHiddenBills >= payment.amount) {
                            paidConsumedByHiddenBills -= payment.amount
                        } else if (paidConsumedByHiddenBills > 0.0) {
                            val remainingAmount = payment.amount - paidConsumedByHiddenBills
                            if (remainingAmount > 0.0) {
                                remainingPayments.add(payment.copy(amount = remainingAmount))
                            }
                            paidConsumedByHiddenBills = 0.0
                        } else {
                            remainingPayments.add(payment)
                        }
                    }
                    remainingPayments
                }

                val creditBillsWithItems: List<Pair<Bill, List<BillItem>>> = unpaidBills.reversed().map { bill ->
                    val items = _allBillItems.value.filter { it.billId == bill.id }
                    Pair(bill, items)
                }

                val file = withContext(Dispatchers.IO) {
                    PdfGenerator.ensureTemplateFilesExist(context)
                    PdfGenerator.generatePendingInvoicePdf(
                        context, creditBillsWithItems, paymentsForPdf,
                        shopName, shopAddress, shopPhone,
                        logoBase64, invoiceMessage,
                        customer.name, customerMobile, creditAmount
                    )
                }

                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share Pending Invoice").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (e: Exception) {
                Log.e("LedgerVM", "generateAndSharePendingInvoice failed", e)
            }
        }
    }
}
