package com.shop.billing.ui.screens.billdetail

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shop.billing.data.model.Bill
import com.shop.billing.data.model.BillItem
import com.shop.billing.data.remote.SupabaseClient
import com.shop.billing.data.repository.BillRepository
import com.shop.billing.util.Constants
import com.shop.billing.util.PdfGenerator
import com.shop.billing.util.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class BillDetailState(
    val bill: Bill? = null,
    val items: List<BillItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class BillDetailViewModel @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val billRepository: BillRepository,
    @ApplicationContext private val context: Context,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(BillDetailState())
    val state: StateFlow<BillDetailState> = _state

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    init {
        val billId = savedStateHandle.get<String>("billId")
        if (billId != null) {
            loadBill(billId)
        }
    }

    fun loadBill(billId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val bill = billRepository.getBillById(billId)
                val items = if (bill != null) billRepository.getItemsByBillId(billId) else emptyList()
                _state.value = BillDetailState(bill = bill, items = items, isLoading = false)

                if (bill == null) {
                    val prefs = context.dataStore.data.first()
                    val url = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_URL)] ?: Constants.HARDCODED_SUPABASE_URL
                    val apiKey = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_KEY)] ?: Constants.HARDCODED_SUPABASE_KEY
                    val shopCode = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: ""
                    val (rmtBills, rmtItems) = supabaseClient.pullBills(url, apiKey, shopCode)
                    val rmtBill = rmtBills.find { it.id == billId }
                    if (rmtBill != null) {
                        val rmtBillItems = rmtItems.filter { it.billId == billId }
                        billRepository.insertBillWithItems(rmtBill, rmtBillItems)
                        _state.value = BillDetailState(bill = rmtBill, items = rmtBillItems, isLoading = false)
                    } else {
                        _state.value = BillDetailState(isLoading = false)
                    }
                }
            } catch (e: Exception) {
                Log.e("BillDetailVM", "Failed to load bill $billId", e)
                _state.value = BillDetailState(isLoading = false, error = e.message ?: "Failed to load bill")
            }
        }
    }

    fun deleteBill() {
        val billId = _state.value.bill?.id ?: return
        viewModelScope.launch {
            try {
                billRepository.deleteBill(billId)
                val prefs = context.dataStore.data.first()
                val url = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_URL)] ?: Constants.HARDCODED_SUPABASE_URL
                val apiKey = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_KEY)] ?: Constants.HARDCODED_SUPABASE_KEY
                val shopCode = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: ""
                withContext(Dispatchers.IO) {
                    supabaseClient.deleteBill(url, apiKey, shopCode, billId)
                }
                _state.value = BillDetailState(isLoading = false)
            } catch (e: Exception) {
                Log.e("BillDetailVM", "Failed to delete bill $billId", e)
                _state.value = _state.value.copy(error = e.message ?: "Failed to delete bill")
            }
        }
    }

    fun generatePdf(shopName: String, shopAddress: String, shopPhone: String) {
        val s = _state.value
        val bill = s.bill ?: return
        if (_isGenerating.value) return

        _isGenerating.value = true
        viewModelScope.launch {
            try {
                val logoBase64 = withContext(Dispatchers.IO) {
                    readLogoBase64()
                }
                val invoiceMessage = withContext(Dispatchers.IO) {
                    readInvoiceMessage()
                }
                val file = withContext(Dispatchers.IO) {
                    PdfGenerator.ensureTemplateFilesExist(context)
                    PdfGenerator.generateVectorInvoice(
                        context, bill, s.items, shopName, shopAddress, shopPhone, logoBase64, invoiceMessage
                    )
                }
                _isGenerating.value = false

                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share Invoice").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (e: Exception) {
                Log.e("BillDetailVM", "PDF generation failed", e)
                _isGenerating.value = false
            }
        }
    }

    private suspend fun readLogoBase64(): String? {
        return try {
            val prefs = context.dataStore.data.first()
            prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_LOGO)]
        } catch (_: Exception) { null }
    }

    private suspend fun readInvoiceMessage(): String {
        return try {
            val prefs = context.dataStore.data.first()
            prefs[stringPreferencesKey(Constants.SETTINGS_KEY_INVOICE_MESSAGE)] ?: ""
        } catch (_: Exception) { "" }
    }

    suspend fun getShopSettings(): Triple<String, String, String> {
        return try {
            val prefs = context.dataStore.data.first()
            val name = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_NAME)] ?: Constants.DEFAULT_SHOP_NAME
            val address = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_ADDRESS)] ?: Constants.DEFAULT_SHOP_ADDRESS
            val phone = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_PHONE)] ?: Constants.DEFAULT_SHOP_PHONE
            Triple(name, address, phone)
        } catch (e: Exception) {
            Triple(Constants.DEFAULT_SHOP_NAME, Constants.DEFAULT_SHOP_ADDRESS, Constants.DEFAULT_SHOP_PHONE)
        }
    }
}
