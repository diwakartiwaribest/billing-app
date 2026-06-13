package com.shop.billing.data.remote

import android.util.Log
import com.shop.billing.data.model.ShopItem
import com.shop.billing.data.model.Bill
import com.shop.billing.data.model.BillItem
import com.shop.billing.data.model.Customer
import com.shop.billing.data.model.CustomerPayment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class SupabaseClient @Inject constructor() {

    companion object {
        private const val TAG = "SupabaseClient"

        private fun millisToIso(millis: Long): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            return sdf.format(millis)
        }

        internal fun isoToMillis(iso: String): Long {
            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                sdf.parse(iso)?.time ?: 0L
            } catch (e: Exception) {
                try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                    sdf.parse(iso)?.time ?: 0L
                } catch (e2: Exception) {
                    System.currentTimeMillis()
                }
            }
        }
    }

    private fun connect(
        url: String,
        apiKey: String,
        table: String,
        method: String,
        body: String? = null,
        prefer: String? = null
    ): String? {
        val conn = URL("${url}/rest/v1/$table").openConnection() as HttpURLConnection
        conn.setRequestProperty("apikey", apiKey)
        conn.setRequestProperty("Authorization", "Bearer $apiKey")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Prefer", prefer ?: "return=representation")
        conn.requestMethod = method
        conn.connectTimeout = 15000
        conn.readTimeout = 15000

        if (body != null && (method == "POST" || method == "PATCH" || method == "DELETE")) {
            conn.doOutput = true
            OutputStreamWriter(conn.outputStream).use { it.write(body) }
        }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val response = stream?.bufferedReader()?.use(BufferedReader::readText) ?: ""

        if (code !in 200..299) {
            Log.e(TAG, "HTTP $code for $table $method: $response")
            throw Exception("Supabase error $code: $response")
        }

        return response.takeIf { it.isNotEmpty() }
    }

    private fun connectFast(
        url: String,
        apiKey: String,
        table: String,
        method: String,
        body: String? = null,
        prefer: String? = null
    ): String? {
        val conn = URL("${url}/rest/v1/$table").openConnection() as HttpURLConnection
        conn.setRequestProperty("apikey", apiKey)
        conn.setRequestProperty("Authorization", "Bearer $apiKey")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Prefer", prefer ?: "return=minimal")
        conn.requestMethod = method
        conn.connectTimeout = 5000
        conn.readTimeout = 5000

        if (body != null && (method == "POST" || method == "PATCH" || method == "DELETE")) {
            conn.doOutput = true
            OutputStreamWriter(conn.outputStream).use { it.write(body) }
        }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val response = stream?.bufferedReader()?.use(BufferedReader::readText) ?: ""

        if (code !in 200..299) {
            Log.e(TAG, "HTTP $code for $table $method: $response")
            throw Exception("Supabase error $code: $response")
        }

        return response.takeIf { it.isNotEmpty() }
    }

    private fun upsert(url: String, apiKey: String, table: String, body: String) {
        connect(url, apiKey, table, "POST", body,
            prefer = "return=minimal,resolution=merge-duplicates")
    }

    // ── Shops ──────────────────────────────────────────────

    fun createShop(url: String, apiKey: String, code: String, secret: String) {
        val body = JSONObject().apply {
            put("code", code)
            put("secret", secret)
        }.toString()
        upsert(url, apiKey, "shops", body)
    }

    fun validateSecret(url: String, apiKey: String, code: String, secret: String): Boolean {
        return try {
            val result = connect(url, apiKey, "shops?code=eq.$code&secret=eq.$secret&select=code", "GET")
            if (result == null) false
            else {
                val arr = JSONArray(result)
                arr.length() > 0
            }
        } catch (e: Exception) {
            Log.e(TAG, "validateSecret failed", e)
            false
        }
    }

    fun shopExists(url: String, apiKey: String, code: String): Boolean {
        return try {
            val result = connect(url, apiKey, "shops?code=eq.$code&select=code", "GET")
            if (result == null) false
            else {
                val arr = JSONArray(result)
                arr.length() > 0
            }
        } catch (e: Exception) {
            Log.e(TAG, "shopExists failed", e)
            false
        }
    }

    fun ensureShopExists(url: String, apiKey: String, code: String, secret: String) {
        if (!shopExists(url, apiKey, code)) {
            createShop(url, apiKey, code, secret)
        }
    }

    fun generateSecret(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return buildString { repeat(32) { append(chars[Random.nextInt(chars.length)]) } }
    }

    // ── Bills ──────────────────────────────────────────────

    fun pushBill(url: String, apiKey: String, shopCode: String, bill: Bill, items: List<BillItem>) {
        val billObj = JSONObject().apply {
            put("id", bill.id)
            put("shop_code", shopCode)
            put("bill_number", bill.billNumber)
            put("customer_name", bill.customerName)
            put("customer_mobile", bill.customerMobile)
            put("total_amount", bill.totalAmount)
            put("created_at", millisToIso(bill.createdAt))
            put("created_by", bill.createdBy)
            put("payment_status", bill.paymentStatus)
        }
        try {
            upsert(url, apiKey, "bills", billObj.toString())
        } catch (e: Exception) {
            Log.e(TAG, "pushBill failed for bill ${bill.id}", e)
        }

        for (item in items) {
            try {
                val itemObj = JSONObject().apply {
                    put("id", item.id)
                    put("bill_id", item.billId)
                    put("shop_code", shopCode)
                    put("item_name", item.itemName)
                    put("quantity", item.quantity)
                    put("unit_price", item.unitPrice)
                    put("subtotal", item.subtotal)
                }
                upsert(url, apiKey, "bill_items", itemObj.toString())
            } catch (e: Exception) {
                Log.e(TAG, "pushBillItem failed for item ${item.id}", e)
            }
        }
    }

    suspend     fun pushAllBills(url: String, apiKey: String, shopCode: String, bills: List<Bill>, getItemsForBill: suspend (String) -> List<BillItem>) {
        for (bill in bills) {
            try {
                val items = getItemsForBill(bill.id)
                pushBill(url, apiKey, shopCode, bill, items)
            } catch (e: Exception) {
                Log.e(TAG, "pushAllBills failed for bill ${bill.id}", e)
            }
        }
    }

    suspend fun pullBills(url: String, apiKey: String, shopCode: String): Pair<List<Bill>, List<BillItem>> = withContext(Dispatchers.IO) {
        try {
            val billsResult = connect(url, apiKey, "bills?shop_code=eq.$shopCode&select=*", "GET")
            val itemsResult = connect(url, apiKey, "bill_items?shop_code=eq.$shopCode&select=*", "GET")

            val bills = mutableListOf<Bill>()
            val billItems = mutableListOf<BillItem>()

            if (billsResult != null) {
                val arr = JSONArray(billsResult)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    bills.add(Bill(
                        id = obj.getString("id"),
                        billNumber = obj.getString("bill_number"),
                        customerName = obj.getString("customer_name"),
                        customerMobile = if (obj.has("customer_mobile")) obj.getString("customer_mobile") else "",
                        totalAmount = obj.getDouble("total_amount"),
                        createdAt = if (obj.has("created_at")) isoToMillis(obj.getString("created_at")) else System.currentTimeMillis(),
                        createdBy = if (obj.has("created_by")) obj.getString("created_by") else "",
                        paymentStatus = if (obj.has("payment_status")) obj.getString("payment_status") else "paid"
                    ))
                }
            }

            if (itemsResult != null) {
                val arr = JSONArray(itemsResult)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    billItems.add(BillItem(
                        id = obj.getString("id"),
                        billId = obj.getString("bill_id"),
                        itemName = obj.getString("item_name"),
                        quantity = obj.getInt("quantity"),
                        unitPrice = obj.getDouble("unit_price"),
                        subtotal = obj.getDouble("subtotal")
                    ))
                }
            }

            Pair(bills, billItems)
        } catch (e: Exception) {
            Log.e(TAG, "pullBills failed", e)
            Pair(emptyList(), emptyList())
        }
    }

    fun deleteAllBillItemsForShop(url: String, apiKey: String, shopCode: String) {
        try {
            connectFast(url, apiKey, "bill_items?shop_code=eq.$shopCode", "DELETE", null)
        } catch (e: Exception) {
            Log.e(TAG, "deleteAllBillItemsForShop failed", e)
        }
    }

    fun deleteAllBillsForShop(url: String, apiKey: String, shopCode: String) {
        try {
            connectFast(url, apiKey, "bills?shop_code=eq.$shopCode", "DELETE", null)
        } catch (e: Exception) {
            Log.e(TAG, "deleteAllBillsForShop failed", e)
        }
    }

    fun deleteBillsNotInIds(url: String, apiKey: String, shopCode: String, ids: List<String>) {
        try {
            if (ids.isEmpty()) {
                connectFast(url, apiKey, "bills?shop_code=eq.$shopCode", "DELETE", null)
                return
            }
            val allRemote = connect(url, apiKey, "bills?shop_code=eq.$shopCode&select=id", "GET") ?: return
            val arr = JSONArray(allRemote)
            val remoteIds = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                remoteIds.add(arr.getJSONObject(i).getString("id"))
            }
            val toDelete = remoteIds.filter { it !in ids }
            if (toDelete.isNotEmpty()) {
                val delStr = toDelete.joinToString(",")
                connectFast(url, apiKey, "bill_items?shop_code=eq.$shopCode&bill_id=in.($delStr)", "DELETE", null)
                connectFast(url, apiKey, "bills?shop_code=eq.$shopCode&id=in.($delStr)", "DELETE", null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "deleteBillsNotInIds failed", e)
        }
    }

    fun deleteBillItemsNotInBillIds(url: String, apiKey: String, shopCode: String, billIds: List<String>) {
        try {
            if (billIds.isEmpty()) {
                connectFast(url, apiKey, "bill_items?shop_code=eq.$shopCode", "DELETE", null)
                return
            }
            val allRemote = connect(url, apiKey, "bill_items?shop_code=eq.$shopCode&select=bill_id", "GET") ?: return
            val arr = JSONArray(allRemote)
            val remoteBillIds = mutableSetOf<String>()
            for (i in 0 until arr.length()) {
                remoteBillIds.add(arr.getJSONObject(i).getString("bill_id"))
            }
            val toDelete = remoteBillIds.filter { it !in billIds }
            if (toDelete.isNotEmpty()) {
                val delStr = toDelete.joinToString(",")
                connectFast(url, apiKey, "bill_items?shop_code=eq.$shopCode&bill_id=in.($delStr)", "DELETE", null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "deleteBillItemsNotInBillIds failed", e)
        }
    }

    fun deleteCustomersNotInMobiles(url: String, apiKey: String, shopCode: String, mobiles: List<String>) {
        try {
            if (mobiles.isEmpty()) {
                connectFast(url, apiKey, "customers?shop_code=eq.$shopCode", "DELETE", null)
                return
            }
            val allRemote = connect(url, apiKey, "customers?shop_code=eq.$shopCode&select=mobile", "GET") ?: return
            val arr = JSONArray(allRemote)
            val remoteMobiles = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                remoteMobiles.add(arr.getJSONObject(i).getString("mobile"))
            }
            val toDelete = remoteMobiles.filter { it !in mobiles }
            for (m in toDelete) {
                connectFast(url, apiKey, "customers?shop_code=eq.$shopCode&mobile=eq.${java.net.URLEncoder.encode(m, "UTF-8")}", "DELETE", null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "deleteCustomersNotInMobiles failed", e)
        }
    }

    fun deleteAllCustomerPaymentsForShop(url: String, apiKey: String, shopCode: String) {
        try {
            connectFast(url, apiKey, "customer_payments?shop_code=eq.$shopCode", "DELETE", null)
        } catch (e: Exception) {
            Log.e(TAG, "deleteAllCustomerPaymentsForShop failed", e)
        }
    }

    fun deleteBillsByIds(url: String, apiKey: String, shopCode: String, ids: String) {
        try {
            connectFast(url, apiKey, "bill_items?shop_code=eq.$shopCode&bill_id=in.($ids)", "DELETE", null)
            connectFast(url, apiKey, "bills?shop_code=eq.$shopCode&id=in.($ids)", "DELETE", null)
        } catch (e: Exception) {
            Log.e(TAG, "deleteBillsByIds failed", e)
        }
    }

    // ── Shop Items ─────────────────────────────────────────

    fun pushShopItem(url: String, apiKey: String, shopCode: String, item: ShopItem) {
        val obj = JSONObject().apply {
            put("id", item.id)
            put("shop_code", shopCode)
            put("name", item.name)
            put("price", item.price)
            put("category", item.category)
            put("created_at", millisToIso(item.createdAt))
        }
        upsert(url, apiKey, "shop_items", obj.toString())
    }

    fun pushAllShopItems(url: String, apiKey: String, shopCode: String, items: List<ShopItem>) {
        for (item in items) {
            try {
                pushShopItem(url, apiKey, shopCode, item)
            } catch (e: Exception) {
                Log.e(TAG, "pushShopItem failed for ${item.name}", e)
            }
        }
    }

    suspend fun pullShopItems(url: String, apiKey: String, shopCode: String): List<ShopItem> = withContext(Dispatchers.IO) {
        try {
            val result = connect(url, apiKey, "shop_items?shop_code=eq.$shopCode&select=*", "GET") ?: return@withContext emptyList()
            val arr = JSONArray(result)
            val items = mutableListOf<ShopItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                items.add(ShopItem(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    price = obj.getDouble("price"),
                    category = obj.getString("category"),
                    createdAt = if (obj.has("created_at")) isoToMillis(obj.getString("created_at")) else System.currentTimeMillis()
                ))
            }
            items
        } catch (e: Exception) {
            Log.e(TAG, "pullShopItems failed", e)
            emptyList()
        }
    }

    fun deleteShopItemsByCategory(url: String, apiKey: String, shopCode: String, category: String) {
        try {
            connectFast(url, apiKey, "shop_items?shop_code=eq.$shopCode&category=eq.${java.net.URLEncoder.encode(category, "UTF-8")}", "DELETE", null)
        } catch (e: Exception) {
            Log.e(TAG, "deleteShopItemsByCategory failed", e)
        }
    }

    fun deleteShopItemsNotInIds(url: String, apiKey: String, shopCode: String, ids: List<String>) {
        try {
            if (ids.isEmpty()) {
                connectFast(url, apiKey, "shop_items?shop_code=eq.$shopCode", "DELETE", null)
                return
            }
            val allRemote = connect(url, apiKey, "shop_items?shop_code=eq.$shopCode&select=id", "GET") ?: return
            val arr = JSONArray(allRemote)
            val remoteIds = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                remoteIds.add(arr.getJSONObject(i).getString("id"))
            }
            val toDelete = remoteIds.filter { it !in ids }
            if (toDelete.isNotEmpty()) {
                val delStr = toDelete.joinToString(",")
                connectFast(url, apiKey, "shop_items?shop_code=eq.$shopCode&id=in.($delStr)", "DELETE", null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "deleteShopItemsNotInIds failed", e)
        }
    }

    // ── Settings ───────────────────────────────────────────

    fun pushSettings(url: String, apiKey: String, shopCode: String, name: String, address: String, phone: String, logo: String, includeLogo: Boolean = true, invoiceMessage: String = "", categories: String = "") {
        val obj = JSONObject().apply {
            put("shop_code", shopCode)
            put("shop_name", name)
            put("shop_address", address)
            put("shop_phone", phone)
            put("invoice_message", invoiceMessage)
            if (categories.isNotBlank()) put("categories", categories)
            if (includeLogo) put("shop_logo", logo)
        }
        upsert(url, apiKey, "shop_settings", obj.toString())
    }

    suspend fun pullSettings(url: String, apiKey: String, shopCode: String, includeLogo: Boolean = true): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val fields = if (includeLogo) "*" else "shop_code,shop_name,shop_address,shop_phone,invoice_message,categories"
            val result = connect(url, apiKey, "shop_settings?shop_code=eq.$shopCode&select=$fields", "GET") ?: return@withContext null
            val arr = JSONArray(result)
            if (arr.length() > 0) arr.getJSONObject(0) else null
        } catch (e: Exception) {
            Log.e(TAG, "pullSettings failed", e)
            null
        }
    }

    // ── Management API: Auto-create tables ────────────────

    // ── Auth (Supabase Auth REST API) ─────────────────────

    fun signUp(url: String, apiKey: String, email: String, password: String): JSONObject? {
        val conn = URL("${url}/auth/v1/signup").openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("apikey", apiKey)
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 15000
        conn.readTimeout = 15000

        val body = JSONObject().apply {
            put("email", email)
            put("password", password)
        }.toString()
        OutputStreamWriter(conn.outputStream).use { it.write(body) }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val response = stream?.bufferedReader()?.use(BufferedReader::readText) ?: ""

        if (code !in 200..299) {
            Log.e(TAG, "signUp HTTP $code: $response")
            throw Exception("Sign up failed: $response")
        }

        Log.d(TAG, "signUp response: $response")
        return if (response.isNotEmpty()) JSONObject(response) else null
    }

    fun signIn(url: String, apiKey: String, email: String, password: String): JSONObject? {
        val conn = URL("${url}/auth/v1/token?grant_type=password").openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("apikey", apiKey)
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 15000
        conn.readTimeout = 15000

        val body = JSONObject().apply {
            put("email", email)
            put("password", password)
        }.toString()
        OutputStreamWriter(conn.outputStream).use { it.write(body) }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val response = stream?.bufferedReader()?.use(BufferedReader::readText) ?: ""

        if (code !in 200..299) {
            Log.e(TAG, "signIn HTTP $code: $response")
            throw Exception("Sign in failed: $response")
        }

        return if (response.isNotEmpty()) JSONObject(response) else null
    }

    // ── User Shops (role management) ──────────────────────

    fun registerUserShop(url: String, apiKey: String, userId: String, shopCode: String, role: String, deviceName: String = "", email: String = "") {
        try {
            val existing = connect(url, apiKey, "user_shops?user_id=eq.$userId&shop_code=eq.$shopCode&select=user_id", "GET")
            val arr = if (existing != null) JSONArray(existing) else JSONArray()
            if (arr.length() > 0) {
                val body = JSONObject().apply {
                    put("role", role)
                    put("email", email)
                }.toString()
                connect(url, apiKey, "user_shops?user_id=eq.$userId&shop_code=eq.$shopCode", "PATCH", body)
            } else {
                val obj = JSONObject().apply {
                    put("user_id", userId)
                    put("shop_code", shopCode)
                    put("role", role)
                    put("device_name", deviceName)
                    put("email", email)
                }
                connect(url, apiKey, "user_shops", "POST", obj.toString(),
                    prefer = "return=minimal,resolution=merge-duplicates")
            }
        } catch (e: Exception) {
            Log.e(TAG, "registerUserShop failed", e)
        }
    }

    fun pullUserShops(url: String, apiKey: String, shopCode: String): JSONArray {
        val result = connect(url, apiKey, "user_shops?shop_code=eq.$shopCode&select=*", "GET")
        return if (result != null) JSONArray(result) else JSONArray()
    }

    fun updateUserShopField(url: String, apiKey: String, userId: String, shopCode: String, field: String, value: String) {
        connect(url, apiKey, "user_shops?user_id=eq.$userId&shop_code=eq.$shopCode", "PATCH", "{\"$field\":\"$value\"}")
    }

    fun getUserRole(url: String, apiKey: String, shopCode: String, userId: String): String {
        return try {
            val result = connect(url, apiKey, "user_shops?shop_code=eq.$shopCode&user_id=eq.$userId&select=role", "GET")
            if (result != null) {
                val arr = JSONArray(result)
                if (arr.length() > 0) arr.getJSONObject(0).getString("role") else "member"
            } else "member"
        } catch (e: Exception) {
            Log.e(TAG, "getUserRole failed", e)
            "member"
        }
    }

    fun transferOwnership(url: String, apiKey: String, shopCode: String, newOwnerUserId: String, currentOwnerUserId: String) {
        val newOwnerObj = JSONObject().apply {
            put("user_id", newOwnerUserId)
            put("shop_code", shopCode)
            put("role", "owner")
        }
        upsert(url, apiKey, "user_shops", newOwnerObj.toString())

        val currentOwnerObj = JSONObject().apply {
            put("user_id", currentOwnerUserId)
            put("shop_code", shopCode)
            put("role", "member")
        }
        upsert(url, apiKey, "user_shops", currentOwnerObj.toString())
    }

    fun removeMember(url: String, apiKey: String, shopCode: String, userId: String) {
        try {
            connect(url, apiKey, "user_shops?shop_code=eq.$shopCode&user_id=eq.$userId", "DELETE", null)
        } catch (e: Exception) {
            Log.e(TAG, "removeMember failed", e)
        }
    }

    fun leaveShop(url: String, apiKey: String, shopCode: String, userId: String) {
        removeMember(url, apiKey, shopCode, userId)
    }

    fun deleteShop(url: String, apiKey: String, shopCode: String) {
        try {
            connect(url, apiKey, "bill_items?shop_code=eq.$shopCode", "DELETE", null)
            connect(url, apiKey, "bills?shop_code=eq.$shopCode", "DELETE", null)
            connect(url, apiKey, "shop_items?shop_code=eq.$shopCode", "DELETE", null)
            connect(url, apiKey, "user_shops?shop_code=eq.$shopCode", "DELETE", null)
            connect(url, apiKey, "shop_settings?shop_code=eq.$shopCode", "DELETE", null)
        } catch (e: Exception) {
            Log.e(TAG, "deleteShop failed", e)
        }
    }

    fun getMemberCount(url: String, apiKey: String, shopCode: String): Int {
        return try {
            val result = connect(url, apiKey, "user_shops?shop_code=eq.$shopCode&select=user_id", "GET")
            if (result != null) JSONArray(result).length() else 0
        } catch (e: Exception) {
            Log.e(TAG, "getMemberCount failed", e)
            0
        }
    }

    fun isOwnerInSupabase(url: String, apiKey: String, shopCode: String): Boolean {
        return try {
            val result = connect(url, apiKey, "user_shops?shop_code=eq.$shopCode&role=eq.owner&select=user_id", "GET")
            if (result != null) JSONArray(result).length() > 0 else false
        } catch (e: Exception) {
            Log.e(TAG, "isOwnerInSupabase failed", e)
            false
        }
    }

    fun claimOwnershipIfNoOwner(url: String, apiKey: String, shopCode: String, userId: String) {
        try {
            if (isOwnerInSupabase(url, apiKey, shopCode)) return
            val obj = JSONObject().apply {
                put("user_id", userId)
                put("shop_code", shopCode)
                put("role", "owner")
            }
            upsert(url, apiKey, "user_shops", obj.toString())
            Log.d(TAG, "Auto-claimed ownership for $userId on shop $shopCode")
        } catch (e: Exception) {
            Log.e(TAG, "claimOwnershipIfNoOwner failed", e)
        }
    }

    // ── Customers ─────────────────────────────────────────

    fun pushAllCustomers(url: String, apiKey: String, shopCode: String, customers: List<Customer>) {
        for (c in customers) {
            try {
                val obj = JSONObject().apply {
                    put("shop_code", shopCode)
                    put("name", c.name)
                    put("mobile", c.mobile)
                    put("total_bills", c.totalBills)
                    put("total_spent", c.totalSpent)
                    put("pending_amount", c.pendingAmount)
                    put("created_at", millisToIso(c.createdAt))
                }
                connect(url, apiKey, "customers?on_conflict=mobile,shop_code", "POST", obj.toString(),
                    prefer = "return=minimal,resolution=merge-duplicates")
            } catch (e: Exception) {
                Log.e(TAG, "pushCustomer failed for ${c.mobile}", e)
            }
        }
    }

    suspend fun pullCustomers(url: String, apiKey: String, shopCode: String): List<Customer> = withContext(Dispatchers.IO) {
        try {
            val result = connect(url, apiKey, "customers?shop_code=eq.$shopCode&select=*", "GET") ?: return@withContext emptyList()
            val arr = JSONArray(result)
            val list = mutableListOf<Customer>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(Customer(
                    name = obj.optString("name", ""),
                    mobile = obj.getString("mobile"),
                    totalBills = obj.optInt("total_bills", 0),
                    totalSpent = obj.optDouble("total_spent", 0.0),
                    pendingAmount = obj.optDouble("pending_amount", 0.0),
                    createdAt = isoToMillis(obj.optString("created_at", ""))
                ))
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "pullCustomers failed", e)
            emptyList()
        }
    }

    fun fetchAllCustomers(url: String, apiKey: String, shopCode: String): JSONArray {
        return try {
            val result = connect(url, apiKey, "customers?shop_code=eq.$shopCode&select=*&order=total_spent.desc", "GET")
            if (result != null) JSONArray(result) else JSONArray()
        } catch (e: Exception) { JSONArray() }
    }

    fun deleteCustomer(url: String, apiKey: String, customerId: String) {
        connect(url, apiKey, "customers?id=eq.$customerId", "DELETE")
    }

    // ── Customer Payments (Ledger) ───────────────────────

    fun pushAllCustomerPayments(url: String, apiKey: String, shopCode: String, payments: List<CustomerPayment>) {
        for (p in payments) {
            try {
                val obj = JSONObject().apply {
                    put("shop_code", shopCode)
                    put("uuid", p.uuid)
                    put("customer_mobile", p.customerMobile)
                    put("amount", p.amount)
                    put("note", p.note)
                    put("created_at", millisToIso(p.createdAt))
                }
                connect(url, apiKey, "customer_payments", "POST", obj.toString(),
                    prefer = "return=minimal,resolution=merge-duplicates")
            } catch (e: Exception) {
                Log.e(TAG, "pushCustomerPayment failed for ${p.customerMobile}", e)
            }
        }
    }

    suspend fun pullCustomerPayments(url: String, apiKey: String, shopCode: String): List<CustomerPayment> = withContext(Dispatchers.IO) {
        try {
            val result = connect(url, apiKey, "customer_payments?shop_code=eq.$shopCode&select=*", "GET") ?: return@withContext emptyList()
            val arr = JSONArray(result)
            val list = mutableListOf<CustomerPayment>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(CustomerPayment(
                    uuid = if (obj.has("uuid")) obj.getString("uuid") else "",
                    customerMobile = obj.getString("customer_mobile"),
                    amount = obj.getDouble("amount"),
                    note = obj.optString("note", ""),
                    createdAt = isoToMillis(obj.optString("created_at", ""))
                ))
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "pullCustomerPayments failed", e)
            emptyList()
        }
    }

    fun deleteCustomerPayment(url: String, apiKey: String, shopCode: String, paymentId: Long) {
        try {
            connect(url, apiKey, "customer_payments?shop_code=eq.$shopCode&id=eq.$paymentId", "DELETE", null)
        } catch (e: Exception) {
            Log.e(TAG, "deleteCustomerPayment failed", e)
        }
    }

    fun deleteCustomerPaymentByUuid(url: String, apiKey: String, uuid: String) {
        try {
            connect(url, apiKey, "customer_payments?uuid=eq.$uuid", "DELETE", null)
        } catch (e: Exception) {
            Log.e(TAG, "deleteCustomerPaymentByUuid failed", e)
        }
    }

    fun deleteCustomerPaymentByMatch(url: String, apiKey: String, shopCode: String, customerMobile: String, amount: Double, createdAt: String) {
        try {
            val filter = "customer_payments?shop_code=eq.$shopCode&customer_mobile=eq.${java.net.URLEncoder.encode(customerMobile, "UTF-8")}&amount=eq.$amount&created_at=eq.$createdAt"
            connect(url, apiKey, filter, "DELETE", null)
        } catch (e: Exception) {
            Log.e(TAG, "deleteCustomerPaymentByMatch failed", e)
        }
    }

    fun deleteCustomerPaymentByMobileAndAmount(url: String, apiKey: String, shopCode: String, customerMobile: String, amount: Double) {
        try {
            val filter = "customer_payments?shop_code=eq.$shopCode&customer_mobile=eq.${java.net.URLEncoder.encode(customerMobile, "UTF-8")}&amount=eq.$amount"
            connect(url, apiKey, filter, "DELETE", null)
        } catch (e: Exception) {
            Log.e(TAG, "deleteCustomerPaymentByMobileAndAmount failed", e)
        }
    }

    fun deleteAllCustomerPaymentsForCustomer(url: String, apiKey: String, shopCode: String, customerMobile: String) {
        try {
            connect(url, apiKey, "customer_payments?shop_code=eq.$shopCode&customer_mobile=eq.$customerMobile", "DELETE", null)
        } catch (e: Exception) {
            Log.e(TAG, "deleteAllCustomerPaymentsForCustomer failed", e)
        }
    }

    fun deleteAllBillsForCustomer(url: String, apiKey: String, shopCode: String, customerMobile: String) {
        try {
            connect(url, apiKey, "bills?shop_code=eq.$shopCode&customer_mobile=eq.$customerMobile", "DELETE", null)
        } catch (e: Exception) {
            Log.e(TAG, "deleteAllBillsForCustomer failed", e)
        }
    }

    fun updateCustomerStats(url: String, apiKey: String, shopCode: String, customerMobile: String, totalBills: Int, totalSpent: Double, pendingAmount: Double) {
        try {
            val obj = org.json.JSONObject().apply {
                put("total_bills", totalBills)
                put("total_spent", totalSpent)
                put("pending_amount", pendingAmount)
            }
            connect(url, apiKey, "customers?shop_code=eq.$shopCode&mobile=eq.$customerMobile", "PATCH", obj.toString())
        } catch (e: Exception) {
            Log.e(TAG, "updateCustomerStats failed", e)
        }
    }

    // ── Management API: Auto-create tables ────────────────

    suspend fun createTablesViaManagementApi(
        personalAccessToken: String,
        projectRef: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val sql = """
CREATE TABLE IF NOT EXISTS shops (
  code TEXT PRIMARY KEY,
  secret TEXT NOT NULL,
  created_at TIMESTAMPTZ DEFAULT now()
);
CREATE TABLE IF NOT EXISTS bills (
  id TEXT PRIMARY KEY,
  shop_code TEXT NOT NULL,
  bill_number TEXT,
  customer_name TEXT DEFAULT '',
  customer_mobile TEXT DEFAULT '',
  total_amount REAL NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ DEFAULT now(),
  created_by TEXT DEFAULT '',
  payment_status TEXT DEFAULT 'paid'
);
CREATE TABLE IF NOT EXISTS bill_items (
  id TEXT PRIMARY KEY,
  bill_id TEXT NOT NULL,
  shop_code TEXT NOT NULL,
  item_name TEXT NOT NULL,
  quantity INTEGER NOT NULL DEFAULT 1,
  unit_price REAL NOT NULL DEFAULT 0,
  subtotal REAL NOT NULL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS shop_items (
  id TEXT PRIMARY KEY,
  shop_code TEXT NOT NULL,
  name TEXT NOT NULL,
  price REAL NOT NULL DEFAULT 0,
  category TEXT DEFAULT 'General',
  created_at TIMESTAMPTZ DEFAULT now()
);
CREATE TABLE IF NOT EXISTS shop_settings (
  shop_code TEXT PRIMARY KEY,
  shop_name TEXT DEFAULT '',
  shop_address TEXT DEFAULT '',
  shop_phone TEXT DEFAULT '',
  shop_logo TEXT DEFAULT '',
  invoice_message TEXT DEFAULT ''
);
CREATE TABLE IF NOT EXISTS customers (
  id BIGINT PRIMARY KEY,
  shop_code TEXT NOT NULL,
  name TEXT DEFAULT '',
  mobile TEXT NOT NULL,
  total_bills INTEGER DEFAULT 0,
  total_spent REAL DEFAULT 0,
  pending_amount REAL DEFAULT 0,
  created_at TIMESTAMPTZ DEFAULT now(),
  UNIQUE(mobile, shop_code)
);
CREATE TABLE IF NOT EXISTS customer_payments (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  shop_code TEXT NOT NULL,
  customer_mobile TEXT NOT NULL,
  amount REAL NOT NULL DEFAULT 0,
  note TEXT DEFAULT '',
  created_at TIMESTAMPTZ DEFAULT now()
);
ALTER TABLE shops ENABLE ROW LEVEL SECURITY;
ALTER TABLE bills ENABLE ROW LEVEL SECURITY;
ALTER TABLE bill_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE shop_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE shop_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE customers ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE shop_settings ADD COLUMN IF NOT EXISTS categories TEXT DEFAULT '';
DO $$ BEGIN
  CREATE POLICY "Allow all" ON shops FOR ALL USING (true) WITH CHECK (true);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;
DO $$ BEGIN
  CREATE POLICY "Allow all" ON bills FOR ALL USING (true) WITH CHECK (true);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;
DO $$ BEGIN
  CREATE POLICY "Allow all" ON bill_items FOR ALL USING (true) WITH CHECK (true);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;
DO $$ BEGIN
  CREATE POLICY "Allow all" ON shop_items FOR ALL USING (true) WITH CHECK (true);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;
DO $$ BEGIN
  CREATE POLICY "Allow all" ON shop_settings FOR ALL USING (true) WITH CHECK (true);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;
DO $$ BEGIN
  CREATE POLICY "Allow all" ON customers FOR ALL USING (true) WITH CHECK (true);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;
DO $$ BEGIN
  CREATE POLICY "Allow all" ON customer_payments FOR ALL USING (true) WITH CHECK (true);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;
CREATE TABLE IF NOT EXISTS user_shops (
  user_id TEXT NOT NULL,
  shop_code TEXT NOT NULL,
  role TEXT DEFAULT 'member',
  device_name TEXT DEFAULT '',
  email TEXT DEFAULT '',
  created_at TIMESTAMPTZ DEFAULT now(),
  PRIMARY KEY (user_id, shop_code)
);
ALTER TABLE user_shops ADD COLUMN IF NOT EXISTS email TEXT DEFAULT '';
ALTER TABLE bills ADD COLUMN IF NOT EXISTS created_by TEXT DEFAULT '';
ALTER TABLE bills ADD COLUMN IF NOT EXISTS payment_status TEXT DEFAULT 'paid';
ALTER TABLE customers ADD COLUMN IF NOT EXISTS pending_amount REAL DEFAULT 0;
ALTER TABLE user_shops ENABLE ROW LEVEL SECURITY;
DO $$ BEGIN
  CREATE POLICY "Allow all" ON user_shops FOR ALL USING (true) WITH CHECK (true);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;""".trimIndent()

            val url = "https://api.supabase.com/v1/projects/$projectRef/database/query"
            val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $personalAccessToken")
            conn.doOutput = true
            conn.connectTimeout = 30000
            conn.readTimeout = 30000

            val body = JSONObject().put("query", sql).toString()
            conn.outputStream.use { it.write(body.toByteArray()) }

            val code = conn.responseCode
            Log.d(TAG, "Management API response: $code")
            conn.disconnect()
            code == 200 || code == 201 || code == 204
        } catch (e: Exception) {
            Log.e(TAG, "createTablesViaManagementApi failed", e)
            false
        }
    }

    fun enableRealtimePublication(personalAccessToken: String, projectRef: String): Boolean {
        val sql = """
ALTER PUBLICATION supabase_realtime ADD TABLE IF NOT EXISTS
  bills, bill_items, shop_items, customers, customer_payments, shop_settings, user_shops;
    """.trimIndent()
        val result = queryManagementApi(personalAccessToken, projectRef, sql)
        return result != null
    }

    fun getTableCreationSql(): String {
        return """
CREATE TABLE IF NOT EXISTS shops (
  code TEXT PRIMARY KEY,
  secret TEXT NOT NULL,
  created_at TIMESTAMPTZ DEFAULT now()
);
CREATE TABLE IF NOT EXISTS bills (
  id TEXT PRIMARY KEY,
  shop_code TEXT NOT NULL,
  bill_number TEXT,
  customer_name TEXT DEFAULT '',
  customer_mobile TEXT DEFAULT '',
  total_amount REAL NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ DEFAULT now(),
  created_by TEXT DEFAULT '',
  payment_status TEXT DEFAULT 'paid'
);
CREATE TABLE IF NOT EXISTS bill_items (
  id TEXT PRIMARY KEY,
  bill_id TEXT NOT NULL,
  shop_code TEXT NOT NULL,
  item_name TEXT NOT NULL,
  quantity INTEGER NOT NULL DEFAULT 1,
  unit_price REAL NOT NULL DEFAULT 0,
  subtotal REAL NOT NULL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS shop_items (
  id TEXT PRIMARY KEY,
  shop_code TEXT NOT NULL,
  name TEXT NOT NULL,
  price REAL NOT NULL DEFAULT 0,
  category TEXT DEFAULT 'General',
  created_at TIMESTAMPTZ DEFAULT now()
);
CREATE TABLE IF NOT EXISTS shop_settings (
  shop_code TEXT PRIMARY KEY,
  shop_name TEXT DEFAULT '',
  shop_address TEXT DEFAULT '',
  shop_phone TEXT DEFAULT '',
  shop_logo TEXT DEFAULT '',
  invoice_message TEXT DEFAULT ''
);
CREATE TABLE IF NOT EXISTS customers (
  id BIGINT PRIMARY KEY,
  shop_code TEXT NOT NULL,
  name TEXT DEFAULT '',
  mobile TEXT NOT NULL,
  total_bills INTEGER DEFAULT 0,
  total_spent REAL DEFAULT 0,
  pending_amount REAL DEFAULT 0,
  created_at TIMESTAMPTZ DEFAULT now(),
  UNIQUE(mobile, shop_code)
);
CREATE TABLE IF NOT EXISTS customer_payments (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  shop_code TEXT NOT NULL,
  customer_mobile TEXT NOT NULL,
  amount REAL NOT NULL DEFAULT 0,
  note TEXT DEFAULT '',
  created_at TIMESTAMPTZ DEFAULT now()
);
ALTER TABLE shops ENABLE ROW LEVEL SECURITY;
ALTER TABLE bills ENABLE ROW LEVEL SECURITY;
ALTER TABLE bill_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE shop_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE shop_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE customers ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE shop_settings ADD COLUMN IF NOT EXISTS categories TEXT DEFAULT '';
CREATE POLICY "Allow all" ON shops FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow all" ON bills FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow all" ON bill_items FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow all" ON shop_items FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow all" ON shop_settings FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow all" ON customers FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow all" ON customer_payments FOR ALL USING (true) WITH CHECK (true);
CREATE TABLE IF NOT EXISTS user_shops (
  user_id TEXT NOT NULL,
  shop_code TEXT NOT NULL,
  role TEXT DEFAULT 'member',
  device_name TEXT DEFAULT '',
  email TEXT DEFAULT '',
  created_at TIMESTAMPTZ DEFAULT now(),
  PRIMARY KEY (user_id, shop_code)
);
ALTER TABLE user_shops ADD COLUMN IF NOT EXISTS email TEXT DEFAULT '';
ALTER TABLE bills ADD COLUMN IF NOT EXISTS created_by TEXT DEFAULT '';
ALTER TABLE bills ADD COLUMN IF NOT EXISTS payment_status TEXT DEFAULT 'paid';
ALTER TABLE customers ADD COLUMN IF NOT EXISTS pending_amount REAL DEFAULT 0;
ALTER TABLE user_shops ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Allow all" ON user_shops FOR ALL USING (true) WITH CHECK (true);
ALTER PUBLICATION supabase_realtime ADD TABLE IF NOT EXISTS
  bills, bill_items, shop_items, customers, customer_payments, shop_settings, user_shops;
""".trimIndent()
    }

    fun getRowCount(url: String, apiKey: String, table: String, shopCode: String): Int {
        val selectField = when (table) {
            "user_shops" -> "user_id"
            "shop_settings" -> "shop_code"
            else -> "id"
        }
        return try {
            val result = connect(url, apiKey, "$table?shop_code=eq.$shopCode&select=$selectField", "GET", prefer = "count=exact")
            if (result != null) {
                val arr = JSONArray(result)
                arr.length()
            } else 0
        } catch (e: Exception) {
            0
        }
    }

    fun getAllRowCounts(url: String, apiKey: String, shopCode: String): Map<String, Int> {
        return mapOf(
            "bills" to getRowCount(url, apiKey, "bills", shopCode),
            "bill_items" to getRowCount(url, apiKey, "bill_items", shopCode),
            "shop_items" to getRowCount(url, apiKey, "shop_items", shopCode),
            "user_shops" to getRowCount(url, apiKey, "user_shops", shopCode),
            "shop_settings" to getRowCount(url, apiKey, "shop_settings", shopCode)
        )
    }

    fun queryManagementApi(personalAccessToken: String, projectRef: String, sql: String): String? {
        return try {
            val url = "https://api.supabase.com/v1/projects/$projectRef/database/query"
            val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $personalAccessToken")
            conn.doOutput = true
            conn.connectTimeout = 30000
            conn.readTimeout = 30000

            val body = JSONObject().put("query", sql).toString()
            conn.outputStream.use { it.write(body.toByteArray()) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }
            conn.disconnect()
            response
        } catch (e: Exception) {
            Log.e(TAG, "queryManagementApi failed", e)
            null
        }
    }

    fun getDatabaseStats(personalAccessToken: String, projectRef: String): Map<String, Any> {
        val stats = mutableMapOf<String, Any>()
        try {
            val sizeResult = queryManagementApi(personalAccessToken, projectRef, "SELECT pg_database_size(current_database()) as size")
            if (sizeResult != null) {
                val arr = JSONArray(sizeResult)
                if (arr.length() > 0) {
                    val obj = arr.getJSONObject(0)
                    val bytes = obj.getLong("size")
                    stats["db_size_bytes"] = bytes
                    stats["db_size_mb"] = String.format("%.2f", bytes / 1024.0 / 1024.0)
                }
            }
        } catch (e: Exception) { Log.e(TAG, "get db size failed", e) }

        try {
            val authResult = queryManagementApi(personalAccessToken, projectRef, "SELECT count(*) as count FROM auth.users")
            if (authResult != null) {
                val arr = JSONArray(authResult)
                if (arr.length() > 0) {
                    stats["auth_users"] = arr.getJSONObject(0).getInt("count")
                }
            }
        } catch (e: Exception) { Log.e(TAG, "get auth users failed", e) }

        try {
            val tableSizesResult = queryManagementApi(personalAccessToken, projectRef,
                """SELECT relname as table_name,
                   pg_total_relation_size(relid) as total_bytes,
                   pg_relation_size(relid) as data_bytes,
                   n_live_tup as row_count
                   FROM pg_stat_user_tables
                   WHERE schemaname = 'public'
                   ORDER BY pg_total_relation_size(relid) DESC""")
            if (tableSizesResult != null) {
                val arr = JSONArray(tableSizesResult)
                val tables = mutableListOf<Map<String, Any>>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    tables.add(mapOf(
                        "name" to obj.getString("table_name"),
                        "total_bytes" to obj.getLong("total_bytes"),
                        "data_bytes" to obj.getLong("data_bytes"),
                        "row_count" to obj.getLong("row_count")
                    ))
                }
                stats["tables"] = tables
            }
        } catch (e: Exception) { Log.e(TAG, "get table sizes failed", e) }

        try {
            val relResult = queryManagementApi(personalAccessToken, projectRef, "SELECT count(*) as count FROM pg_stat_activity WHERE state = 'active'")
            if (relResult != null) {
                val arr = JSONArray(relResult)
                if (arr.length() > 0) {
                    stats["active_connections"] = arr.getJSONObject(0).getInt("count")
                }
            }
        } catch (e: Exception) { Log.e(TAG, "get connections failed", e) }

        try {
            val dbSizeResult = queryManagementApi(personalAccessToken, projectRef, "SELECT current_database() as name, version() as version, pg_size_pretty(pg_database_size(current_database())) as size_pretty")
            if (dbSizeResult != null) {
                val arr = JSONArray(dbSizeResult)
                if (arr.length() > 0) {
                    val obj = arr.getJSONObject(0)
                    stats["db_name"] = obj.getString("name")
                    stats["db_version"] = obj.getString("version")
                    stats["db_size_pretty"] = obj.getString("size_pretty")
                }
            }
        } catch (e: Exception) { Log.e(TAG, "get db info failed", e) }

        return stats
    }

    fun fetchAllBills(url: String, apiKey: String, shopCode: String): JSONArray {
        return try {
            val result = connect(url, apiKey, "bills?shop_code=eq.$shopCode&select=id,bill_number,customer_name,customer_mobile,total_amount,created_at,created_by,shop_code&order=created_at.desc", "GET")
            if (result != null) JSONArray(result) else JSONArray()
        } catch (e: Exception) { JSONArray() }
    }

    fun fetchAllBillItems(url: String, apiKey: String, shopCode: String): JSONArray {
        return try {
            val result = connect(url, apiKey, "bill_items?shop_code=eq.$shopCode&select=id,bill_id,shop_code,item_name,quantity,unit_price,subtotal", "GET")
            if (result != null) JSONArray(result) else JSONArray()
        } catch (e: Exception) { JSONArray() }
    }

    fun fetchAllShopItems(url: String, apiKey: String, shopCode: String): JSONArray {
        return try {
            val result = connect(url, apiKey, "shop_items?shop_code=eq.$shopCode&select=*&order=name.asc", "GET")
            if (result != null) JSONArray(result) else JSONArray()
        } catch (e: Exception) { JSONArray() }
    }

    fun fetchAllUserShops(url: String, apiKey: String, shopCode: String): JSONArray {
        return try {
            val result = connect(url, apiKey, "user_shops?shop_code=eq.$shopCode&select=*", "GET")
            if (result != null) JSONArray(result) else JSONArray()
        } catch (e: Exception) { JSONArray() }
    }

    fun fetchShopSettings(url: String, apiKey: String, shopCode: String): JSONObject? {
        return try {
            val result = connect(url, apiKey, "shop_settings?shop_code=eq.$shopCode&select=*", "GET")
            if (result != null) {
                val arr = JSONArray(result)
                if (arr.length() > 0) arr.getJSONObject(0) else null
            } else null
        } catch (e: Exception) { null }
    }

    fun deleteBill(url: String, apiKey: String, shopCode: String, billId: String) {
        try {
            connect(url, apiKey, "bill_items?shop_code=eq.$shopCode&bill_id=eq.$billId", "DELETE", null)
            connect(url, apiKey, "bills?shop_code=eq.$shopCode&id=eq.$billId", "DELETE", null)
        } catch (e: Exception) {
            Log.e(TAG, "deleteBill failed", e)
        }
    }

    fun deleteShopItem(url: String, apiKey: String, shopCode: String, itemId: String) {
        try {
            connect(url, apiKey, "shop_items?shop_code=eq.$shopCode&id=eq.$itemId", "DELETE", null)
        } catch (e: Exception) {
            Log.e(TAG, "deleteShopItem failed", e)
        }
    }

    fun updateShopItem(url: String, apiKey: String, shopCode: String, itemId: String, name: String, price: Double, category: String) {
        try {
            val body = JSONObject().apply {
                put("name", name)
                put("price", price)
                put("category", category)
            }.toString()
            connect(url, apiKey, "shop_items?shop_code=eq.$shopCode&id=eq.$itemId", "PATCH", body)
        } catch (e: Exception) {
            Log.e(TAG, "updateShopItem failed", e)
        }
    }

    fun updateShopSettings(url: String, apiKey: String, shopCode: String, field: String, value: String) {
        try {
            val body = JSONObject().apply { put(field, value) }.toString()
            connect(url, apiKey, "shop_settings?shop_code=eq.$shopCode", "PATCH", body)
        } catch (e: Exception) {
            Log.e(TAG, "updateShopSettings failed", e)
        }
    }

    fun fetchAllShops(url: String, apiKey: String): JSONArray {
        return try {
            val result = connect(url, apiKey, "shops?select=*&order=code.asc", "GET")
            if (result != null) JSONArray(result) else JSONArray()
        } catch (e: Exception) {
            Log.e(TAG, "fetchAllShops failed", e)
            JSONArray()
        }
    }

    fun deleteShopByCode(url: String, apiKey: String, code: String) {
        try {
            connect(url, apiKey, "bill_items?shop_code=eq.$code", "DELETE", null)
            connect(url, apiKey, "bills?shop_code=eq.$code", "DELETE", null)
            connect(url, apiKey, "shop_items?shop_code=eq.$code", "DELETE", null)
            connect(url, apiKey, "user_shops?shop_code=eq.$code", "DELETE", null)
            connect(url, apiKey, "shop_settings?shop_code=eq.$code", "DELETE", null)
            connect(url, apiKey, "shops?code=eq.$code", "DELETE", null)
        } catch (e: Exception) {
            Log.e(TAG, "deleteShopByCode failed", e)
        }
    }
}
