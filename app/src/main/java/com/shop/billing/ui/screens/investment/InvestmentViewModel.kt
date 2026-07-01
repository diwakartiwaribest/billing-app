package com.shop.billing.ui.screens.investment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shop.billing.data.local.entity.InvestmentEntity
import com.shop.billing.data.repository.InvestmentRepository
import com.shop.billing.util.dataStore
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import com.shop.billing.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class InvestmentViewModel @Inject constructor(
    private val investmentRepository: InvestmentRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _investments = MutableStateFlow<List<InvestmentEntity>>(emptyList())
    val investments: StateFlow<List<InvestmentEntity>> = _investments

    private val _totalInvestment = MutableStateFlow(0.0)
    val totalInvestment: StateFlow<Double> = _totalInvestment

    private var currentShopCode = ""

    init {
        viewModelScope.launch {
            val prefs = context.dataStore.data.first()
            currentShopCode = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: ""

            if (currentShopCode.isNotBlank()) {
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
            }
        }
    }

    fun addInvestment(amount: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            investmentRepository.add(amount, currentShopCode)
        }
    }

    fun deleteInvestment(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            investmentRepository.deleteById(id)
        }
    }
}
