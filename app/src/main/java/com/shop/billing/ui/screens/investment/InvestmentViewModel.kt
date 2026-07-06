package com.shop.billing.ui.screens.investment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shop.billing.data.local.entity.InvestmentEntity
import com.shop.billing.data.local.entity.ProductEntity
import com.shop.billing.data.repository.InvestmentRepository
import com.shop.billing.data.remote.FirebaseClient
import com.shop.billing.data.repository.ProductRepository
import com.shop.billing.data.sync.SyncEngine
import com.shop.billing.util.dataStore
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.shop.billing.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import com.shop.billing.data.model.ShopItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.NonCancellable
import org.json.JSONArray
import javax.inject.Inject

data class PurchaseItem(
    val productId: String,
    val productName: String,
    val quantity: Int,
    val purchasePrice: Double,
    val sellingPrice: Double,
    val barcode: String,
    val category: String = ""
)

data class ResolvedProduct(
    val product: ProductEntity,
    val lastPurchasePrice: Double
)

@HiltViewModel
class InvestmentViewModel @Inject constructor(
    private val investmentRepository: InvestmentRepository,
    private val productRepository: ProductRepository,
    private val firebaseClient: FirebaseClient,
    private val syncEngine: SyncEngine,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _investments = MutableStateFlow<List<InvestmentEntity>>(emptyList())
    val investments: StateFlow<List<InvestmentEntity>> = _investments

    private val _totalInvestment = MutableStateFlow(0.0)
    val totalInvestment: StateFlow<Double> = _totalInvestment

    private val _knownBarcodes = MutableStateFlow<List<String>>(emptyList())
    val knownBarcodes: StateFlow<List<String>> = _knownBarcodes

    private val _allCategories = MutableStateFlow<List<String>>(emptyList())
    val allCategories: StateFlow<List<String>> = _allCategories

    private var currentShopCode = ""

    init {
        viewModelScope.launch {
            val prefs = context.dataStore.data.first()
            currentShopCode = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: ""

            if (currentShopCode.isNotBlank()) {
                val existingProducts = productRepository.getAll(currentShopCode)
                _knownBarcodes.value = existingProducts.filter { it.barcode.isNotBlank() }.map { it.barcode }
                    _allCategories.value = getExistingCategories()

                launch {
                    investmentRepository.observeAll(currentShopCode).collect { list ->
                        _investments.value = list
                    }
                }
                launch {
                    investmentRepository.observeTotal(currentShopCode).collect { total ->
                        _totalInvestment.value = total
                    }
                }
                launch {
                    productRepository.observeAll(currentShopCode).collect { products ->
                        _knownBarcodes.value = products.filter { it.barcode.isNotBlank() }.map { it.barcode }
                        _allCategories.value = getExistingCategories()
                    }
                }
            }
        }
    }

    fun loadBarcodes(onLoaded: (List<String>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val barcodes = _knownBarcodes.value
            withContext(Dispatchers.Main) { onLoaded(barcodes) }
        }
    }

    fun lookupByBarcode(barcode: String, onResult: (ProductEntity?) -> Unit) {
        if (currentShopCode.isBlank()) {
            onResult(null)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val product = productRepository.getByBarcode(barcode.trim(), currentShopCode)
                ?: productRepository.getByBarcodeTrimmed(barcode.trim())
            withContext(Dispatchers.Main) { onResult(product) }
        }
    }

    suspend fun resolveBarcode(barcode: String): ResolvedProduct? = withContext(Dispatchers.IO) {
        if (currentShopCode.isBlank()) return@withContext null
        val product = productRepository.getByBarcode(barcode.trim(), currentShopCode)
            ?: productRepository.getByBarcodeTrimmed(barcode.trim())
        if (product == null) return@withContext null
        val lastPrice = investmentRepository.getLatestPurchasePrice(product.id, currentShopCode) ?: product.buyingPrice
        ResolvedProduct(product, lastPrice)
    }

    suspend fun getAllProducts(): List<ResolvedProduct> = withContext(Dispatchers.IO) {
        if (currentShopCode.isBlank()) return@withContext emptyList()
        productRepository.getAll(currentShopCode).map { product ->
        val lastPrice = investmentRepository.getLatestPurchasePrice(product.id, currentShopCode) ?: product.buyingPrice
            ResolvedProduct(product, lastPrice)
        }
    }

    suspend fun getExistingCategories(): List<String> = withContext(Dispatchers.IO) {
        if (currentShopCode.isBlank()) return@withContext emptyList()
        try {
            val prefs = context.dataStore.data.first()
            val json = prefs[stringPreferencesKey("custom_categories")] ?: "[]"
            val arr = JSONArray(json)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) list.add(arr.getString(i))
            list.filter { it.isNotBlank() }
        } catch (_: Exception) {
            emptyList<String>()
        }
    }

    private suspend fun addCustomCategory(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        try {
            val prefs = context.dataStore.data.first()
            val json = prefs[stringPreferencesKey("custom_categories")] ?: "[]"
            val arr = JSONArray(json)
            val existing = mutableListOf<String>()
            for (i in 0 until arr.length()) existing.add(arr.getString(i))
            if (!existing.contains(trimmed)) {
                existing.add(trimmed)
                context.dataStore.edit { p ->
                    p[stringPreferencesKey("custom_categories")] = JSONArray(existing).toString()
                }
                if (currentShopCode.isNotBlank()) {
                    withContext(kotlinx.coroutines.NonCancellable) {
                        syncEngine.pushCustomCategoriesNow(currentShopCode)
                    }
                }
            }
        } catch (_: Exception) { }
    }

    fun recordPurchases(items: List<PurchaseItem>) {
        if (items.isEmpty() || currentShopCode.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            items.forEach { item ->
                var pid = item.productId
                if (pid.isBlank()) {
                    val newItem = ShopItem(
                        name = item.productName,
                        sellingPrice = item.sellingPrice,
                        buyingPrice = item.purchasePrice,
                        category = item.category,
                        barcode = item.barcode,
                        stockQuantity = 0,
                        lowStockThreshold = 10
                    )
                    productRepository.create(newItem, currentShopCode)
                    val saved = productRepository.getByBarcode(item.barcode, currentShopCode)
                    pid = saved?.id ?: newItem.id
                    if (item.category.isNotBlank()) {
                        addCustomCategory(item.category)
                    }
                }
                investmentRepository.recordProductPurchase(
                    productId = pid,
                    productName = item.productName,
                    quantity = item.quantity,
                    purchasePrice = item.purchasePrice,
                    sellingPriceAtPurchase = item.sellingPrice,
                    barcode = item.barcode,
                    shopCode = currentShopCode
                )
            }
        }
    }

    fun deleteInvestment(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            investmentRepository.deleteById(id)
            if (currentShopCode.isNotBlank()) {
                firebaseClient.deleteInvestmentRemote(currentShopCode, id)
            }
        }
    }

    fun deleteInvestments(ids: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            ids.forEach { id ->
                investmentRepository.deleteById(id)
                if (currentShopCode.isNotBlank()) {
                    firebaseClient.deleteInvestmentRemote(currentShopCode, id)
                }
            }
        }
    }
}
