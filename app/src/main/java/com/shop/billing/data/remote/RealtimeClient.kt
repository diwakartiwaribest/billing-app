package com.shop.billing.data.remote

import android.util.Log
import com.shop.billing.data.repository.BillRepository
import com.shop.billing.data.repository.ShopItemRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RealtimeClient(
    private val supabaseClient: SupabaseClient,
    private val billRepository: BillRepository,
    private val shopItemRepository: ShopItemRepository
) {
    private var job: Job? = null
    private var pollIntervalMs = 30000L

    fun startPolling(scope: CoroutineScope, url: String, apiKey: String, shopCode: String) {
        stopPolling()
        job = scope.launch {
            while (true) {
                try {
                    pullFromSupabase(url, apiKey, shopCode)
                } catch (e: Exception) {
                    Log.e("RealtimeClient", "Polling failed", e)
                }
                delay(pollIntervalMs)
            }
        }
    }

    fun stopPolling() {
        job?.cancel()
        job = null
    }

    private suspend fun pullFromSupabase(url: String, apiKey: String, shopCode: String) {
        try {
            val (bills, billItems) = supabaseClient.pullBills(url, apiKey, shopCode)
            if (bills.isNotEmpty()) {
                billRepository.insertBills(bills)
                billRepository.insertBillItems(billItems)
            }

            val items = supabaseClient.pullShopItems(url, apiKey, shopCode)
            if (items.isNotEmpty()) {
                shopItemRepository.insertItems(items)
            }
        } catch (e: Exception) {
            Log.e("RealtimeClient", "pullFromSupabase failed", e)
        }
    }
}
