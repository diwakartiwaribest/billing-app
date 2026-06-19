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
import com.shop.billing.data.model.MobileTotal
import com.shop.billing.data.repository.CustomerPaymentRepository
import com.shop.billing.data.repository.CustomerRepository
import com.shop.billing.data.repository.InvoiceRepository
import com.shop.billing.data.sync.SyncEngine
import com.shop.billing.util.Constants
import com.shop.billing.util.PdfGenerator
import com.shop.billing.util.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@OptIn(kotlinx.coroutines.FlowPreview::class)
@HiltViewModel
class CustomerLedgerViewModel @Inject constructor(
    private val customerRepository: CustomerRepository,
    private val invoiceRepository: InvoiceRepository,
    private val paymentRepository: CustomerPaymentRepository,
    private val syncEngine: SyncEngine,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _pendingDeletedPayment = MutableStateFlow<CustomerPayment?>(null)
    val pendingDeletedPayment: StateFlow<CustomerPayment?> = _pendingDeletedPayment

    private var _pendingDeletedPaymentMobile = ""

    private val _userRole = MutableStateFlow("member")
    val userRole: StateFlow<String> = _userRole

    private val _allCustomers = MutableStateFlow<List<Customer>>(emptyList())

    private val _totalPaidAggregate = MutableStateFlow(0.0)

    val totalPending: StateFlow<Double> = _allCustomers
        .map { list -> list.sumOf { it.pendingAmount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalPaid: StateFlow<Double> = _totalPaidAggregate

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

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var currentShopCode = ""

    init {
        viewModelScope.launch {
            val prefs = context.dataStore.data.first()
            currentShopCode = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: ""
            _userRole.value = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ROLE)] ?: "member"

            if (currentShopCode.isNotBlank()) {
                // Load from local DB immediately
                loadFullLedger()
                
                // Sync in background
                launch {
                    syncEngine.pushPending(currentShopCode)
                }
                
                // Reactive refresh: recalculate when customers, payments, or invoices change
                launch {
                    combine(
                        customerRepository.observeAll(currentShopCode),
                        paymentRepository.observeCount(currentShopCode),
                        invoiceRepository.observeCount(currentShopCode)
                    ) { _, _, _ -> Unit }
                        .debounce(300)
                        .distinctUntilChanged()
                        .collect {
                            try {
                                loadFullLedger()
                            } catch (e: Exception) {
                                Log.e("LedgerVM", "Combine-triggered loadFullLedger failed", e)
                            }
                        }
                }


            }
        }
    }

    private suspend fun loadFullLedger() {
        _isLoading.value = true
        val recalculated: List<Customer>
        val totalPaid: Double
        withContext(Dispatchers.Default) {
            val customers = customerRepository.getAll(currentShopCode).map { it.toCustomer() }
            val creditTotals = invoiceRepository.getCreditTotalsByMobile(currentShopCode)
            val paymentTotals = paymentRepository.getPaymentTotalsByMobile(currentShopCode)
            totalPaid = paymentTotals.sumOf { it.total }
            // O(1) lookup maps instead of O(n) .find inside .map
            val creditByMobile = creditTotals.associate { it.customerMobile to it.total }
            val paymentByMobile = paymentTotals.associate { it.customerMobile to it.total }
            recalculated = customers.map { c ->
                val creditTotal = creditByMobile[c.mobile] ?: 0.0
                val paidTotal = paymentByMobile[c.mobile] ?: 0.0
                val pending = (creditTotal - paidTotal).coerceAtLeast(0.0)
                val credit = (paidTotal - creditTotal).coerceAtLeast(0.0)
                c.copy(pendingAmount = pending, creditAmount = credit)
            }
        }
        _allCustomers.value = recalculated
        _totalPaidAggregate.value = totalPaid
        _isLoading.value = false
    }

    private fun triggerSync() {
        viewModelScope.launch {
            withContext(NonCancellable) {
                syncEngine.pushPending(currentShopCode)
            }
        }
    }

    private fun applyRecalculated(customers: List<Customer>, creditTotals: List<MobileTotal>, paymentTotals: List<MobileTotal>) {
        val creditByMobile = creditTotals.associate { it.customerMobile to it.total }
        val paymentByMobile = paymentTotals.associate { it.customerMobile to it.total }
        val recalculated = customers.map { c ->
            val creditTotal = creditByMobile[c.mobile] ?: 0.0
            val totalPaid = paymentByMobile[c.mobile] ?: 0.0
            val pending = (creditTotal - totalPaid).coerceAtLeast(0.0)
            val credit = (totalPaid - creditTotal).coerceAtLeast(0.0)
            c.copy(pendingAmount = pending, creditAmount = credit)
        }
        _allCustomers.value = recalculated
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun addPayment(customerMobile: String, amount: Double, note: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                val payment = CustomerPayment(
                    customerMobile = customerMobile,
                    amount = amount,
                    note = note
                )
                paymentRepository.create(payment, currentShopCode)
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
                triggerSync()
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
            paymentRepository.softDelete(payment.uuid, currentShopCode)
            triggerSync()
        }
        _pendingDeletedPayment.value = null
        _pendingDeletedPaymentMobile = ""
    }

    fun clearPaymentHistory(customerMobile: String) {
        if (_userRole.value != "owner" && _userRole.value != "admin") return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val payments = paymentRepository.getByCustomerMobile(customerMobile)
                for (p in payments) {
                    paymentRepository.softDelete(p.uuid, currentShopCode)
                }
                val invoices = invoiceRepository.getByCustomerMobile(customerMobile)
                for (inv in invoices) {
                    invoiceRepository.softDelete(inv.id, currentShopCode)
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
                // Force UI refresh immediately (reactive flows may not fire in time)
                withContext(Dispatchers.Default) { loadFullLedger() }
                triggerSync()
            } catch (e: Exception) {
                Log.e("LedgerVM", "clearPaymentHistory failed", e)
            }
        }
    }

    fun getPaymentsForCustomer(mobile: String): StateFlow<List<CustomerPayment>> {
        return paymentsCache.getOrPut(mobile) {
            paymentRepository.observeByCustomerMobile(mobile, currentShopCode)
                .map { it.map { e -> e.toCustomerPayment() } }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        }
    }

    fun getBillsForCustomer(mobile: String): StateFlow<List<Bill>> {
        return billsCache.getOrPut(mobile) {
            invoiceRepository.observeByCustomerMobile(mobile, currentShopCode)
                .map { it.map { e -> e.toBill() }.filter { b -> b.paymentStatus != "invoice" } }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        }
    }

    fun getTotalPaidForCustomer(mobile: String): StateFlow<Double> {
        return totalPaidCache.getOrPut(mobile) {
            paymentRepository.observeByCustomerMobile(mobile, currentShopCode)
                .map { it.sumOf { p -> p.amount } }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
        }
    }

    fun syncPayments() {
        triggerSync()
    }

    fun generateAndSharePendingInvoice(customerMobile: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val prefs = context.dataStore.data.first()
                val shopName = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_NAME)] ?: Constants.DEFAULT_SHOP_NAME
                val shopAddress = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_ADDRESS)] ?: Constants.DEFAULT_SHOP_ADDRESS
                val shopPhone = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_PHONE)] ?: Constants.DEFAULT_SHOP_PHONE
                val logoBase64 = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_LOGO)]
                val invoiceMessage = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_INVOICE_MESSAGE)] ?: ""

                val customer = _allCustomers.value.find { it.mobile == customerMobile } ?: return@launch
                val allBills = invoiceRepository.getByCustomerMobile(customerMobile).map { it.toBill() }
                val allPayments = paymentRepository.getByCustomerMobile(customerMobile).map { it.toCustomerPayment() }
                val creditBills = allBills.filter { it.paymentStatus == "credit" }.sortedBy { it.createdAt }
                val payments = allPayments.sortedBy { it.createdAt }
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
                    val items = invoiceRepository.getItemsByInvoice(bill.id).map { it.toBillItem() }
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

    fun addCustomer(name: String, mobile: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val customer = Customer(name = name, mobile = mobile)
                customerRepository.create(customer, currentShopCode)
                triggerSync()
            } catch (e: Exception) {
                Log.e("LedgerVM", "addCustomer failed", e)
            }
        }
    }
}
