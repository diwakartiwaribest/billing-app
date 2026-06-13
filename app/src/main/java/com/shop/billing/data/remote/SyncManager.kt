package com.shop.billing.data.remote

import android.util.Log
import com.shop.billing.data.model.Bill
import com.shop.billing.data.model.BillItem
import com.shop.billing.data.model.Customer
import com.shop.billing.data.model.CustomerPayment
import com.shop.billing.data.model.ShopItem
import com.shop.billing.data.repository.BillRepository
import com.shop.billing.data.repository.ShopItemRepository

class SyncManager(
    private val supabaseClient: SupabaseClient,
    private val billRepository: BillRepository,
    private val shopItemRepository: ShopItemRepository
) {
    companion object {
        private const val TAG = "SyncManager"
    }

    suspend fun pushAllToSupabase(url: String, apiKey: String, shopCode: String) {
        Log.d(TAG, "pushAllToSupabase started")
        try {
            billRepository.getAllBills().collect { bills ->
                for (bill in bills) {
                    val items = billRepository.getItemsByBillId(bill.id)
                    supabaseClient.pushBill(url, apiKey, shopCode, bill, items)
                }
                return@collect
            }
        } catch (e: Exception) {
            Log.e(TAG, "pushAllToSupabase failed", e)
        }
        Log.d(TAG, "pushAllToSupabase completed")
    }

    suspend fun pushBillToSupabase(url: String, apiKey: String, shopCode: String, bill: Bill, items: List<BillItem>) {
        try {
            supabaseClient.pushBill(url, apiKey, shopCode, bill, items)
        } catch (e: Exception) {
            Log.e(TAG, "pushBillToSupabase failed for bill ${bill.id}", e)
        }
    }

    suspend fun pushShopItemsToSupabase(url: String, apiKey: String, shopCode: String, items: List<ShopItem>) {
        try {
            for (item in items) {
                supabaseClient.pushShopItem(url, apiKey, shopCode, item)
            }
        } catch (e: Exception) {
            Log.e(TAG, "pushShopItemsToSupabase failed", e)
        }
    }

    suspend fun pullAllFromSupabase(url: String, apiKey: String, shopCode: String): Boolean {
        return try {
            Log.d(TAG, "pullAllFromSupabase started")

            val (bills, billItems) = supabaseClient.pullBills(url, apiKey, shopCode)
            if (bills.isNotEmpty()) {
                billRepository.insertBills(bills)
                billRepository.insertBillItems(billItems)
            }

            val shopItems = supabaseClient.pullShopItems(url, apiKey, shopCode)
            if (shopItems.isNotEmpty()) {
                shopItemRepository.insertItems(shopItems)
            }

            val customers = supabaseClient.pullCustomers(url, apiKey, shopCode)
            val customerPayments = supabaseClient.pullCustomerPayments(url, apiKey, shopCode)

            Log.d(TAG, "pullAllFromSupabase completed: ${bills.size} bills, ${shopItems.size} items")
            true
        } catch (e: Exception) {
            Log.e(TAG, "pullAllFromSupabase failed", e)
            false
        }
    }

    suspend fun pushCustomersToSupabase(url: String, apiKey: String, shopCode: String, customers: List<Customer>) {
        try {
            supabaseClient.pushAllCustomers(url, apiKey, shopCode, customers)
        } catch (e: Exception) {
            Log.e(TAG, "pushCustomersToSupabase failed", e)
        }
    }

    suspend fun pushCustomerPaymentsToSupabase(url: String, apiKey: String, shopCode: String, payments: List<CustomerPayment>) {
        try {
            supabaseClient.pushAllCustomerPayments(url, apiKey, shopCode, payments)
        } catch (e: Exception) {
            Log.e(TAG, "pushCustomerPaymentsToSupabase failed", e)
        }
    }
}
