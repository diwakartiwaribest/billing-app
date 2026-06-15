package com.shop.billing.data

import com.shop.billing.data.model.Bill
import com.shop.billing.data.model.BillItem
import com.shop.billing.data.model.Customer
import com.shop.billing.data.model.CustomerPayment
import com.shop.billing.data.model.ShopItem
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppDataCache @Inject constructor() {
    private val lock = Any()

    @Volatile var itemsLoaded = false; private set
    @Volatile var customersLoaded = false; private set
    @Volatile var billsLoaded = false; private set
    @Volatile var paymentsLoaded = false; private set

    private val _items = mutableListOf<ShopItem>()
    private val _customers = mutableListOf<Customer>()
    private val _bills = mutableListOf<Bill>()
    private val _billItems = mutableListOf<BillItem>()
    private val _payments = mutableListOf<CustomerPayment>()

    val items: List<ShopItem> get() = synchronized(lock) { _items.toList() }
    val customers: List<Customer> get() = synchronized(lock) { _customers.toList() }
    val bills: List<Bill> get() = synchronized(lock) { _bills.toList() }
    val billItems: List<BillItem> get() = synchronized(lock) { _billItems.toList() }
    val payments: List<CustomerPayment> get() = synchronized(lock) { _payments.toList() }

    private val _jsonCache = mutableMapOf<String, JSONArray>()
    fun getJson(key: String): JSONArray? = synchronized(lock) { _jsonCache[key]?.let { JSONArray(it.toString()) } }
    fun setJson(key: String, value: JSONArray) = synchronized(lock) { _jsonCache[key] = JSONArray(value.toString()) }
    fun hasJson(key: String): Boolean = synchronized(lock) { _jsonCache.containsKey(key) }

    fun setItems(list: List<ShopItem>) = synchronized(lock) { _items.clear(); _items.addAll(list); itemsLoaded = true }
    fun setCustomers(list: List<Customer>) = synchronized(lock) { _customers.clear(); _customers.addAll(list); customersLoaded = true }
    fun setBills(bills: List<Bill>, items: List<BillItem>) = synchronized(lock) { _bills.clear(); _bills.addAll(bills); _billItems.clear(); _billItems.addAll(items); billsLoaded = true }
    fun setPayments(list: List<CustomerPayment>) = synchronized(lock) { _payments.clear(); _payments.addAll(list); paymentsLoaded = true }

    /** Populate model cache from raw JSON arrays (used when loading from disk) */
    fun fromJson(itemsJson: JSONArray?, customersJson: JSONArray?, billsJson: JSONArray?, billItemsJson: JSONArray?, paymentsJson: JSONArray?) = synchronized(lock) {
        if (itemsJson != null) {
            _items.clear()
            for (i in 0 until itemsJson.length()) {
                val o = itemsJson.getJSONObject(i)
                _items.add(ShopItem(
                    id = o.optString("id", java.util.UUID.randomUUID().toString()),
                    name = o.getString("name"),
                    price = o.optDouble("price", 0.0),
                    category = o.optString("category", "General"),
                    createdAt = o.optLong("created_at", System.currentTimeMillis())
                ))
            }
            itemsLoaded = true
            _jsonCache["items"] = JSONArray(itemsJson.toString())
        }
        if (customersJson != null) {
            _customers.clear()
            for (i in 0 until customersJson.length()) {
                val o = customersJson.getJSONObject(i)
                _customers.add(Customer(
                    name = o.optString("name", ""),
                    mobile = o.getString("mobile"),
                    totalBills = o.optInt("total_bills", 0),
                    totalSpent = o.optDouble("total_spent", 0.0),
                    pendingAmount = o.optDouble("pending_amount", 0.0),
                    creditAmount = o.optDouble("credit_amount", 0.0),
                    createdAt = o.optLong("created_at", System.currentTimeMillis())
                ))
            }
            customersLoaded = true
            _jsonCache["customers"] = JSONArray(customersJson.toString())
        }
        if (billsJson != null) {
            _bills.clear()
            for (i in 0 until billsJson.length()) {
                val o = billsJson.getJSONObject(i)
                _bills.add(Bill(
                    id = o.optString("id", java.util.UUID.randomUUID().toString()),
                    billNumber = o.optString("bill_number", ""),
                    customerName = o.optString("customer_name", ""),
                    customerMobile = o.optString("customer_mobile", ""),
                    totalAmount = o.optDouble("total_amount", 0.0),
                    createdAt = o.optLong("created_at", System.currentTimeMillis()),
                    createdBy = o.optString("created_by", ""),
                    paymentStatus = o.optString("payment_status", "paid")
                ))
            }
            billsLoaded = true
            _jsonCache["bills"] = JSONArray(billsJson.toString())
        }
        if (billItemsJson != null) {
            _billItems.clear()
            for (i in 0 until billItemsJson.length()) {
                val o = billItemsJson.getJSONObject(i)
                _billItems.add(BillItem(
                    id = o.optString("id", java.util.UUID.randomUUID().toString()),
                    billId = o.getString("bill_id"),
                    itemName = o.getString("item_name"),
                    quantity = o.optInt("quantity", 1),
                    unitPrice = o.optDouble("unit_price", 0.0),
                    subtotal = o.optDouble("subtotal", 0.0)
                ))
            }
            _jsonCache["billItems"] = JSONArray(billItemsJson.toString())
        }
        if (paymentsJson != null) {
            _payments.clear()
            for (i in 0 until paymentsJson.length()) {
                val o = paymentsJson.getJSONObject(i)
                _payments.add(CustomerPayment(
                    customerMobile = o.getString("customer_mobile"),
                    amount = o.optDouble("amount", 0.0),
                    note = o.optString("note", ""),
                    uuid = o.optString("uuid", java.util.UUID.randomUUID().toString()),
                    createdAt = o.optLong("created_at", System.currentTimeMillis())
                ))
            }
            paymentsLoaded = true
            _jsonCache["payments"] = JSONArray(paymentsJson.toString())
        }
    }

    fun toItemsJson(): JSONArray = synchronized(lock) {
        val arr = JSONArray()
        _items.forEach { it -> arr.put(JSONObject().apply {
            put("id", it.id); put("name", it.name); put("price", it.price)
            put("category", it.category); put("created_at", it.createdAt)
        })}
        return arr
    }

    fun toCustomersJson(): JSONArray = synchronized(lock) {
        val arr = JSONArray()
        _customers.forEach { it -> arr.put(JSONObject().apply {
            put("name", it.name); put("mobile", it.mobile)
            put("total_bills", it.totalBills); put("total_spent", it.totalSpent)
            put("pending_amount", it.pendingAmount); put("credit_amount", it.creditAmount)
            put("created_at", it.createdAt)
        })}
        return arr
    }

    fun toBillsJson(): JSONArray = synchronized(lock) {
        val arr = JSONArray()
        _bills.forEach { it -> arr.put(JSONObject().apply {
            put("id", it.id); put("bill_number", it.billNumber)
            put("customer_name", it.customerName); put("customer_mobile", it.customerMobile)
            put("total_amount", it.totalAmount); put("created_at", it.createdAt)
            put("created_by", it.createdBy); put("payment_status", it.paymentStatus)
        })}
        return arr
    }

    fun toBillItemsJson(): JSONArray = synchronized(lock) {
        val arr = JSONArray()
        _billItems.forEach { it -> arr.put(JSONObject().apply {
            put("id", it.id); put("bill_id", it.billId)
            put("item_name", it.itemName); put("quantity", it.quantity)
            put("unit_price", it.unitPrice); put("subtotal", it.subtotal)
        })}
        return arr
    }

    fun toPaymentsJson(): JSONArray = synchronized(lock) {
        val arr = JSONArray()
        _payments.forEach { it -> arr.put(JSONObject().apply {
            put("customer_mobile", it.customerMobile); put("amount", it.amount)
            put("note", it.note); put("uuid", it.uuid); put("created_at", it.createdAt)
        })}
        return arr
    }

    // DB Stats cache for Manage Database section
    @Volatile var dbStatsLoaded = false; private set

    private var _dbStats = emptyMap<String, Int>()
    private var _dbDetails = emptyMap<String, Any>()

    val dbStats: Map<String, Int> get() = synchronized(lock) { _dbStats }
    val dbDetails: Map<String, Any> get() = synchronized(lock) { _dbDetails }

    fun setDbStats(stats: Map<String, Int>, details: Map<String, Any>) = synchronized(lock) {
        _dbStats = stats; _dbDetails = details; dbStatsLoaded = true
    }

    fun setDbDetails(details: Map<String, Any>) = synchronized(lock) {
        _dbDetails = details
    }

    fun invalidateAll() = synchronized(lock) {
        itemsLoaded = false; customersLoaded = false; billsLoaded = false; paymentsLoaded = false
        _items.clear(); _customers.clear(); _bills.clear(); _billItems.clear(); _payments.clear()
        _jsonCache.clear()
    }
}
