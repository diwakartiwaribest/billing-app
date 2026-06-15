package com.shop.billing.ui.screens.history

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shop.billing.data.AppDataCache
import com.shop.billing.data.model.Bill
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val dataCache: AppDataCache,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val startDate = MutableStateFlow<Long?>(null)
    val endDate = MutableStateFlow<Long?>(null)

    val selectedBillIds = MutableStateFlow<Set<String>>(emptySet())
    val isSelectionMode: StateFlow<Boolean> = selectedBillIds.combine(MutableStateFlow(0)) { ids, _ ->
        ids.isNotEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _userRole = MutableStateFlow("member")
    val userRole: StateFlow<String> = _userRole

    private val _allBills = MutableStateFlow<List<Bill>>(emptyList())

    private val _pendingDeletedBills = MutableStateFlow<List<Bill>>(emptyList())
    val pendingDeletedBills: StateFlow<List<Bill>> = _pendingDeletedBills

    private var _pendingDeletedIds = emptySet<String>()

    val bills: StateFlow<List<Bill>> = combine(
        _allBills,
        startDate,
        endDate
    ) { allBills, start, end ->
        if (start == null && end == null) {
            allBills
        } else {
            allBills.filter { bill ->
                val billTime = bill.createdAt
                val afterStart = start == null || billTime >= start
                val beforeEnd = end == null || billTime <= end
                afterStart && beforeEnd
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            val prefs = context.dataStore.data.first()
            _userRole.value = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ROLE)] ?: "member"
        }
        pullFromSupabase()
    }

    private fun pullFromSupabase() {
        if (dataCache.billsLoaded) {
            _allBills.value = dataCache.bills
        }
        viewModelScope.launch {
            try {
                val prefs = context.dataStore.data.first()
                val url = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_URL)] ?: ""
                val key = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_KEY)] ?: ""
                val shopCode = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: ""
                if (url.isBlank() || key.isBlank() || shopCode.isBlank()) return@launch
                val (bills, billItems) = withContext(Dispatchers.IO) {
                    supabaseClient.pullBills(url, key, shopCode)
                }
                _allBills.value = bills
                dataCache.setBills(bills, billItems)
            } catch (e: Exception) {
                Log.e("HistoryVM", "pullFromSupabase failed", e)
            }
        }
    }

    fun toggleSelection(billId: String) {
        val current = selectedBillIds.value.toMutableSet()
        if (current.contains(billId)) {
            current.remove(billId)
        } else {
            current.add(billId)
        }
        selectedBillIds.value = current
    }

    fun selectAll() {
        selectedBillIds.value = bills.value.map { it.id }.toSet()
    }

    fun clearSelection() {
        selectedBillIds.value = emptySet()
    }

    fun deleteSelectedBills() {
        val ids = selectedBillIds.value.toList()
        if (ids.isEmpty()) return
        val billsToDelete = bills.value.filter { it.id in ids }
        _pendingDeletedBills.value = billsToDelete
        _pendingDeletedIds = ids.toSet()
        selectedBillIds.value = emptySet()
    }

    fun undoDeleteBills() {
        _pendingDeletedBills.value = emptyList()
        _pendingDeletedIds = emptySet()
    }

    fun confirmDeleteBills() {
        val ids = _pendingDeletedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            try {
                val prefs = context.dataStore.data.first()
                val url = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_URL)] ?: ""
                val key = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_KEY)] ?: ""
                val shopCode = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: ""
                if (url.isNotBlank() && key.isNotBlank() && shopCode.isNotBlank()) {
                    withContext(Dispatchers.IO) {
                        supabaseClient.deleteBillsByIds(url, key, shopCode, ids.joinToString(","))
                    }
                }
            } catch (e: Exception) {
                Log.e("HistoryVM", "deleteBillsFromSupabase failed", e)
            }
            _allBills.value = _allBills.value.filter { it.id !in ids }
        }
        _pendingDeletedBills.value = emptyList()
        _pendingDeletedIds = emptySet()
    }

    fun deleteBillsInRange() {
        val start = startDate.value
        val end = endDate.value
        if (start == null && end == null) return
        val billsToDelete = bills.value
        val ids = billsToDelete.map { it.id }
        if (ids.isNotEmpty()) {
            _pendingDeletedBills.value = billsToDelete
            _pendingDeletedIds = ids.toSet()
            selectedBillIds.value = emptySet()
        }
    }

    fun confirmDeleteBillsInRange() {
        val ids = _pendingDeletedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            try {
                val prefs = context.dataStore.data.first()
                val url = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_URL)] ?: ""
                val key = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_KEY)] ?: ""
                val shopCode = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: ""
                if (url.isNotBlank() && key.isNotBlank() && shopCode.isNotBlank()) {
                    withContext(Dispatchers.IO) {
                        supabaseClient.deleteBillsByIds(url, key, shopCode, ids.joinToString(","))
                    }
                }
            } catch (e: Exception) {
                Log.e("HistoryVM", "deleteBillsFromSupabase failed", e)
            }
            _allBills.value = _allBills.value.filter { it.id !in ids }
        }
        _pendingDeletedBills.value = emptyList()
        _pendingDeletedIds = emptySet()
    }

    fun clearDateFilter() {
        startDate.value = null
        endDate.value = null
    }
}
