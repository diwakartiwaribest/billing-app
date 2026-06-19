package com.shop.billing.ui.screens.history

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.shop.billing.data.local.entity.InvoiceEntity
import com.shop.billing.data.model.Bill
import com.shop.billing.data.repository.InvoiceRepository
import com.shop.billing.data.sync.SyncEngine
import com.shop.billing.util.Constants
import com.shop.billing.util.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val invoiceRepository: InvoiceRepository,
    private val syncEngine: SyncEngine,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val startDate = MutableStateFlow<Long?>(null)
    val endDate = MutableStateFlow<Long?>(null)

    val selectedBillIds = MutableStateFlow<Set<String>>(emptySet())
    val isSelectionMode: StateFlow<Boolean> = selectedBillIds.combine(MutableStateFlow(0)) { ids, _ ->
        ids.isNotEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val searchQuery = MutableStateFlow("")

    private val _userRole = MutableStateFlow("member")
    val userRole: StateFlow<String> = _userRole

    private val shopCodeFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: ""
    }

    private val userRoleFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ROLE)] ?: "member"
    }

    private val dateFilterFlow: Flow<Pair<Long?, Long?>> = combine(startDate, endDate) { s, e -> Pair(s, e) }

    private val debouncedSearch = searchQuery.debounce(300).distinctUntilChanged()

    private data class PageParams(val shopCode: String, val startDate: Long?, val endDate: Long?, val customerName: String)

    private val pageParamsFlow: Flow<PageParams> = combine(
        shopCodeFlow, dateFilterFlow, debouncedSearch
    ) { code, dates, query ->
        PageParams(code, dates.first, dates.second, query)
    }.filter { it.shopCode.isNotBlank() }

    private val pagedEntityFlow: Flow<PagingData<InvoiceEntity>> = pageParamsFlow
        .flatMapLatest { params ->
            if (params.customerName.isBlank()) {
                invoiceRepository.observePaged(params.shopCode, params.startDate, params.endDate)
            } else {
                invoiceRepository.observePagedWithName(params.shopCode, params.startDate, params.endDate, params.customerName)
            }
        }
        .cachedIn(viewModelScope)

    val pagingDataFlow: Flow<PagingData<Bill>> = pagedEntityFlow
        .map { pd -> pd.map { entity -> entity.toBill() } }
        .cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            _userRole.value = userRoleFlow.first()
        }
    }

    fun toggleSelection(billId: String) {
        val current = selectedBillIds.value.toMutableSet()
        if (current.contains(billId)) current.remove(billId) else current.add(billId)
        selectedBillIds.value = current
    }

    fun selectAll() {
        selectedBillIds.value = emptySet()
    }

    fun clearSelection() {
        selectedBillIds.value = emptySet()
    }

    fun clearDateFilter() {
        startDate.value = null
        endDate.value = null
    }

    fun clearSearch() {
        searchQuery.value = ""
    }

    fun deleteSelectedBills() {
        viewModelScope.launch {
            val shopCode = shopCodeFlow.first()
            selectedBillIds.value.forEach { id ->
                invoiceRepository.softDelete(id, shopCode)
            }
            triggerSync()
            selectedBillIds.value = emptySet()
        }
    }

    fun deleteBillsInRange() {
        viewModelScope.launch {
            val shopCode = shopCodeFlow.first()
            val start = startDate.value
            val end = endDate.value
            val bills = invoiceRepository.getByDateRange(shopCode, start, end)
            bills.forEach { bill ->
                invoiceRepository.softDelete(bill.id, shopCode)
            }
            triggerSync()
        }
    }

    private fun triggerSync() {
        viewModelScope.launch {
            withContext(NonCancellable) {
                val shopCode = shopCodeFlow.first()
                syncEngine.pushPending(shopCode)
            }
        }
    }
}
