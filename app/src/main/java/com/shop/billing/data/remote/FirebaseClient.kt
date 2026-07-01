package com.shop.billing.data.remote

import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.snapshots
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import com.shop.billing.data.model.Bill
import com.shop.billing.data.model.BillItem
import com.shop.billing.data.model.Customer
import com.shop.billing.data.model.CustomerPayment
import com.shop.billing.data.model.ShopItem
import com.shop.billing.data.sync.OperationType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseClient @Inject constructor() {

    companion object {
        private const val TAG = "FirebaseClient"
        private const val CUSTOMERS = "customers"
        private const val BILLS = "bills"
        private const val BILL_ITEMS = "bill_items"
        private const val SHOP_ITEMS = "shop_items"
        private const val CUSTOMER_PAYMENTS = "customer_payments"
    private const val SHOPS = "shops"
    private const val USERS = "users"
    private const val MEMBERS = "members"
    }

    private val db: FirebaseFirestore = Firebase.firestore
    private val scope = CoroutineScope(Dispatchers.IO)

    private val listenerRegistrations = ConcurrentHashMap<String, ListenerRegistration>()

    private fun customersCollection(shopCode: String) = db.collection(CUSTOMERS).document(shopCode).collection("data")
    private fun billsCollection(shopCode: String) = db.collection(BILLS).document(shopCode).collection("data")
    private fun billItemsCollection(shopCode: String) = db.collection(BILL_ITEMS).document(shopCode).collection("data")
    private fun shopItemsCollection(shopCode: String) = db.collection(SHOP_ITEMS).document(shopCode).collection("data")
    private fun paymentsCollection(shopCode: String) = db.collection(CUSTOMER_PAYMENTS).document(shopCode).collection("data")

    suspend fun pullCustomers(shopCode: String): List<Customer> {
        Log.d(TAG, "pullCustomers: shopCode=$shopCode")
        return try {
            val snapshot = customersCollection(shopCode)
                .whereEqualTo("deleted", false)
                .get()
                .await()
            Log.d(TAG, "pullCustomers: fetched ${snapshot.size()} records")
            snapshot.documents.mapNotNull { doc ->
                try {
                    Customer(
                        name = doc.getString("name") ?: "",
                        mobile = doc.getString("mobile") ?: doc.id,
                        totalBills = doc.getLong("totalBills")?.toInt() ?: 0,
                        totalSpent = doc.getDouble("totalSpent") ?: 0.0,
                        pendingAmount = doc.getDouble("pendingAmount") ?: 0.0,
                        creditAmount = doc.getDouble("creditAmount") ?: 0.0,
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis(),
                        deleted = doc.getBoolean("deleted") ?: false,
                        version = doc.getLong("version")?.toInt() ?: 1,
                        ownerId = doc.getString("ownerId") ?: ""
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing customer doc ${doc.id}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "pullCustomers failed: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun pushCustomer(shopCode: String, customer: Customer, operationType: OperationType): Customer? {
        Log.d(TAG, "pushCustomer: shopCode=$shopCode op=$operationType customer=${customer.mobile}")
        return try {
            val docRef = customersCollection(shopCode).document(customer.mobile)
            val now = System.currentTimeMillis()

            val data = hashMapOf(
                "name" to customer.name,
                "mobile" to customer.mobile,
                "totalBills" to customer.totalBills,
                "totalSpent" to customer.totalSpent,
                "pendingAmount" to customer.pendingAmount,
                "creditAmount" to customer.creditAmount,
                "createdAt" to customer.createdAt,
                "updatedAt" to now,
                "deleted" to (operationType == OperationType.DELETE),
                "version" to (customer.version + 1),
                "ownerId" to customer.ownerId
            )

            when (operationType) {
                OperationType.CREATE, OperationType.UPDATE -> {
                    docRef.set(data).await()
                }
                OperationType.DELETE -> {
                    docRef.update("deleted", true, "updatedAt", now, "version", customer.version + 1).await()
                }
            }

            Log.d(TAG, "pushCustomer success: ${customer.mobile}")
            customer.copy(updatedAt = now, version = customer.version + 1, deleted = operationType == OperationType.DELETE)
        } catch (e: Exception) {
            Log.e(TAG, "pushCustomer failed: ${e.message}", e)
            null
        }
    }

    suspend fun deleteCustomer(shopCode: String, mobile: String): Boolean {
        Log.d(TAG, "deleteCustomer: shopCode=$shopCode mobile=$mobile")
        return try {
            customersCollection(shopCode).document(mobile)
                .update("deleted", true, "updatedAt", System.currentTimeMillis())
                .await()
            Log.d(TAG, "deleteCustomer success")
            true
        } catch (e: Exception) {
            Log.e(TAG, "deleteCustomer failed: ${e.message}", e)
            false
        }
    }

    suspend fun pullBills(shopCode: String): Pair<List<Bill>, List<BillItem>> {
        Log.d(TAG, "pullBills: shopCode=$shopCode")
        return try {
            val billsSnapshot = billsCollection(shopCode)
                .whereEqualTo("deleted", false)
                .get()
                .await()

            val itemsSnapshot = billItemsCollection(shopCode)
                .whereEqualTo("deleted", false)
                .get()
                .await()

            Log.d(TAG, "pullBills: fetched ${billsSnapshot.size()} bills, ${itemsSnapshot.size()} items")

            val bills = billsSnapshot.documents.mapNotNull { doc ->
                try {
                    Bill(
                        id = doc.getString("id") ?: doc.id,
                        billNumber = doc.getString("billNumber") ?: "",
                        customerName = doc.getString("customerName") ?: "",
                        customerMobile = doc.getString("customerMobile") ?: "",
                        totalAmount = doc.getDouble("totalAmount") ?: 0.0,
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis(),
                        createdBy = doc.getString("createdBy") ?: "",
                        paymentStatus = doc.getString("paymentStatus") ?: "paid",
                        deleted = doc.getBoolean("deleted") ?: false,
                        version = doc.getLong("version")?.toInt() ?: 1,
                        ownerId = doc.getString("ownerId") ?: ""
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing bill doc ${doc.id}", e)
                    null
                }
            }

            val items = itemsSnapshot.documents.mapNotNull { doc ->
                try {
                    BillItem(
                        id = doc.getString("id") ?: doc.id,
                        billId = doc.getString("billId") ?: "",
                        itemName = doc.getString("itemName") ?: "",
                        quantity = doc.getLong("quantity")?.toInt() ?: 0,
                        unitPrice = doc.getDouble("unitPrice") ?: 0.0,
                        subtotal = doc.getDouble("subtotal") ?: 0.0,
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis(),
                        deleted = doc.getBoolean("deleted") ?: false,
                        version = doc.getLong("version")?.toInt() ?: 1,
                        ownerId = doc.getString("ownerId") ?: ""
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing bill item doc ${doc.id}", e)
                    null
                }
            }

            Pair(bills, items)
        } catch (e: Exception) {
            Log.e(TAG, "pullBills failed: ${e.message}", e)
            Pair(emptyList(), emptyList())
        }
    }

    suspend fun pushBill(shopCode: String, bill: Bill, items: List<BillItem>, operationType: OperationType): Boolean {
        Log.d(TAG, "pushBill: shopCode=$shopCode op=$operationType bill=${bill.id}")
        return try {
            val billDocRef = billsCollection(shopCode).document(bill.id)
            val now = System.currentTimeMillis()

            val billData = hashMapOf(
                "id" to bill.id,
                "billNumber" to bill.billNumber,
                "customerName" to bill.customerName,
                "customerMobile" to bill.customerMobile,
                "totalAmount" to bill.totalAmount,
                "createdAt" to bill.createdAt,
                "updatedAt" to now,
                "createdBy" to bill.createdBy,
                "paymentStatus" to bill.paymentStatus,
                "deleted" to (operationType == OperationType.DELETE),
                "version" to (bill.version + 1),
                "ownerId" to bill.ownerId
            )

            when (operationType) {
                OperationType.CREATE, OperationType.UPDATE -> {
                    billDocRef.set(billData).await()
                }
                OperationType.DELETE -> {
                    billDocRef.update("deleted", true, "updatedAt", now).await()
                }
            }

            for (item in items) {
                val itemDocRef = billItemsCollection(shopCode).document(item.id)
                val itemData = hashMapOf(
                    "id" to item.id,
                    "billId" to item.billId,
                    "itemName" to item.itemName,
                    "quantity" to item.quantity,
                    "unitPrice" to item.unitPrice,
                    "subtotal" to item.subtotal,
                    "createdAt" to item.createdAt,
                    "updatedAt" to now,
                    "deleted" to (operationType == OperationType.DELETE),
                    "version" to (item.version + 1),
                    "ownerId" to item.ownerId
                )
                itemDocRef.set(itemData).await()
            }

            Log.d(TAG, "pushBill success: ${bill.id}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "pushBill failed: ${e.message}", e)
            false
        }
    }

    suspend fun pullShopItems(shopCode: String): List<ShopItem> {
        Log.d(TAG, "pullShopItems: shopCode=$shopCode")
        return try {
            val snapshot = shopItemsCollection(shopCode)
                .whereEqualTo("deleted", false)
                .get()
                .await()
            Log.d(TAG, "pullShopItems: fetched ${snapshot.size()} items")

            snapshot.documents.mapNotNull { doc ->
                try {
                    ShopItem(
                        id = doc.getString("id") ?: doc.id,
                        name = doc.getString("name") ?: "",
                        price = doc.getDouble("price") ?: 0.0,
                                category = doc.getString("category") ?: "",
                        barcode = doc.getString("barcode") ?: "",
                        stockQuantity = doc.getLong("stockQuantity")?.toInt() ?: 0,
                        lowStockThreshold = doc.getLong("lowStockThreshold")?.toInt() ?: 10,
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis(),
                        deleted = doc.getBoolean("deleted") ?: false,
                        version = doc.getLong("version")?.toInt() ?: 1,
                        ownerId = doc.getString("ownerId") ?: ""
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing shop item doc ${doc.id}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "pullShopItems failed: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun pushShopItem(shopCode: String, item: ShopItem, operationType: OperationType): Boolean {
        Log.d(TAG, "pushShopItem: shopCode=$shopCode op=$operationType item=${item.id}")
        return try {
            val docRef = shopItemsCollection(shopCode).document(item.id)
            val now = System.currentTimeMillis()

            val data = hashMapOf(
                "id" to item.id,
                "name" to item.name,
                "price" to item.price,
                "category" to item.category,
                "barcode" to item.barcode,
                "stockQuantity" to item.stockQuantity,
                "lowStockThreshold" to item.lowStockThreshold,
                "createdAt" to item.createdAt,
                "updatedAt" to now,
                "deleted" to (operationType == OperationType.DELETE),
                "version" to (item.version + 1),
                "ownerId" to item.ownerId
            )

            when (operationType) {
                OperationType.CREATE, OperationType.UPDATE -> {
                    docRef.set(data).await()
                }
                OperationType.DELETE -> {
                    docRef.update("deleted", true, "updatedAt", now).await()
                }
            }

            Log.d(TAG, "pushShopItem success: ${item.id}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "pushShopItem failed: ${e.message}", e)
            false
        }
    }

    suspend fun pullCustomerPayments(shopCode: String): List<CustomerPayment> {
        Log.d(TAG, "pullCustomerPayments: shopCode=$shopCode")
        return try {
            val snapshot = paymentsCollection(shopCode)
                .whereEqualTo("deleted", false)
                .get()
                .await()
            Log.d(TAG, "pullCustomerPayments: fetched ${snapshot.size()} payments")

            snapshot.documents.mapNotNull { doc ->
                try {
                    CustomerPayment(
                        id = doc.getLong("id"),
                        uuid = doc.getString("uuid") ?: doc.id,
                        customerMobile = doc.getString("customerMobile") ?: "",
                        amount = doc.getDouble("amount") ?: 0.0,
                        note = doc.getString("note") ?: "",
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis(),
                        deleted = doc.getBoolean("deleted") ?: false,
                        version = doc.getLong("version")?.toInt() ?: 1,
                        ownerId = doc.getString("ownerId") ?: ""
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing payment doc ${doc.id}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "pullCustomerPayments failed: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun pushCustomerPayment(shopCode: String, payment: CustomerPayment, operationType: OperationType): CustomerPayment? {
        Log.d(TAG, "pushCustomerPayment: shopCode=$shopCode op=$operationType uuid=${payment.uuid}")
        return try {
            val docRef = paymentsCollection(shopCode).document(payment.uuid)
            val now = System.currentTimeMillis()

            val data = hashMapOf(
                "id" to payment.id,
                "uuid" to payment.uuid,
                "customerMobile" to payment.customerMobile,
                "amount" to payment.amount,
                "note" to payment.note,
                "createdAt" to payment.createdAt,
                "updatedAt" to now,
                "deleted" to (operationType == OperationType.DELETE),
                "version" to (payment.version + 1),
                "ownerId" to payment.ownerId
            )

            when (operationType) {
                OperationType.CREATE, OperationType.UPDATE -> {
                    docRef.set(data).await()
                }
                OperationType.DELETE -> {
                    docRef.update("deleted", true, "updatedAt", now).await()
                }
            }

            Log.d(TAG, "pushCustomerPayment success: uuid=${payment.uuid}")
            payment.copy(updatedAt = now, version = payment.version + 1, deleted = operationType == OperationType.DELETE)
        } catch (e: Exception) {
            Log.e(TAG, "pushCustomerPayment failed: ${e.message}", e)
            null
        }
    }

    private fun shopsCollection() = db.collection(SHOPS)
    private fun userShopsCollection(userId: String) = db.collection(USERS).document(userId).collection(SHOPS)
    private fun membersCollection(shopCode: String) = shopsCollection().document(shopCode).collection(MEMBERS)

    suspend fun getUserShops(userId: String): List<Pair<String, String>> {
        return try {
            userShopsCollection(userId).get().await().documents.mapNotNull { doc ->
                val role = doc.getString("role") ?: return@mapNotNull null
                doc.id to role
            }
        } catch (e: Exception) {
            Log.e(TAG, "getUserShops failed", e)
            emptyList()
        }
    }

    suspend fun createShop(shopCode: String, ownerId: String, name: String, secret: String, ownerEmail: String = ""): Boolean {
        Log.d(TAG, "createShop: code=$shopCode ownerId=$ownerId")
        return try {
            val shopData = hashMapOf(
                "ownerId" to ownerId,
                "name" to name,
                "secret" to secret,
                "createdAt" to System.currentTimeMillis(),
                "memberIds" to listOf(ownerId)
            )
            shopsCollection().document(shopCode).set(shopData).await()
            userShopsCollection(ownerId).document(shopCode).set(hashMapOf(
                "role" to "owner",
                "joinedAt" to System.currentTimeMillis(),
                "email" to ownerEmail
            )).await()
            membersCollection(shopCode).document(ownerId).set(hashMapOf(
                "role" to "owner",
                "joinedAt" to System.currentTimeMillis(),
                "email" to ownerEmail
            )).await()
            Log.d(TAG, "createShop success: $shopCode")
            true
        } catch (e: Exception) {
            Log.e(TAG, "createShop failed", e)
            false
        }
    }

    suspend fun shopExists(shopCode: String): Boolean {
        return try {
            shopsCollection().document(shopCode).get().await().exists()
        } catch (e: Exception) {
            Log.e(TAG, "shopExists check failed", e)
            false
        }
    }

    suspend fun verifyShopSecret(shopCode: String, secret: String): Boolean {
        return try {
            val doc = shopsCollection().document(shopCode).get().await()
            doc.exists() && doc.getString("secret") == secret
        } catch (e: Exception) {
            Log.e(TAG, "verifyShopSecret failed", e)
            false
        }
    }

    suspend fun addUserToShop(shopCode: String, userId: String, role: String, email: String): Boolean {
        Log.d(TAG, "addUserToShop: code=$shopCode userId=$userId role=$role")
        return try {
            userShopsCollection(userId).document(shopCode).set(hashMapOf(
                "role" to role,
                "joinedAt" to System.currentTimeMillis(),
                "email" to email
            )).await()
            membersCollection(shopCode).document(userId).set(hashMapOf(
                "role" to role,
                "joinedAt" to System.currentTimeMillis(),
                "email" to email
            )).await()
            shopsCollection().document(shopCode).update("memberIds", com.google.firebase.firestore.FieldValue.arrayUnion(userId)).await()
            Log.d(TAG, "addUserToShop success")
            true
        } catch (e: Exception) {
            Log.e(TAG, "addUserToShop failed", e)
            false
        }
    }

    data class ShopMember(
        val userId: String,
        val role: String,
        val email: String,
        val joinedAt: Long
    )

    suspend fun getShopMembers(shopCode: String): List<ShopMember> {
        Log.d(TAG, "getShopMembers: shopCode=$shopCode")
        return try {
            val memberIds = getShopMemberIds(shopCode)
            val memberDocs = membersCollection(shopCode).get().await().documents
            val memberMap = memberDocs.associate { it.id to it }

            // Union: all IDs from subcollection docs + memberIds-only stragglers
            val subIds = memberDocs.map { it.id }.toSet()
            val allIds = (subIds + memberIds).distinct()

            val results = allIds.mapNotNull { id ->
                val doc = memberMap[id]
                if (doc != null) {
                    ShopMember(
                        userId = doc.id,
                        role = doc.getString("role") ?: "member",
                        email = doc.getString("email") ?: "",
                        joinedAt = doc.getLong("joinedAt") ?: 0L
                    )
                } else {
                    // Create placeholder member doc for IDs without a member doc
                    try {
                        membersCollection(shopCode).document(id).set(hashMapOf(
                            "role" to "member",
                            "joinedAt" to System.currentTimeMillis(),
                            "email" to ""
                        )).await()
                    } catch (_: Exception) {}
                    ShopMember(userId = id, role = "member", email = "", joinedAt = 0L)
                }
            }
            Log.d(TAG, "getShopMembers: found ${results.size} members")
            results
        } catch (e: Exception) {
            Log.e(TAG, "getShopMembers failed", e)
            emptyList()
        }
    }

    suspend fun getUserRole(shopCode: String, userId: String): String? {
        return try {
            val doc = userShopsCollection(userId).document(shopCode).get().await()
            doc.getString("role")
        } catch (e: Exception) {
            Log.e(TAG, "getUserRole failed", e)
            null
        }
    }

    suspend fun updateMemberRole(shopCode: String, userId: String, newRole: String) {
        Log.d(TAG, "updateMemberRole: shopCode=$shopCode userId=$userId newRole=$newRole")
        try {
            membersCollection(shopCode).document(userId).update("role", newRole).await()
            userShopsCollection(userId).document(shopCode).update("role", newRole).await()
        } catch (e: Exception) {
            Log.e(TAG, "updateMemberRole failed", e)
        }
    }

    suspend fun updateMemberEmail(shopCode: String, userId: String, email: String) {
        Log.d(TAG, "updateMemberEmail: shopCode=$shopCode userId=$userId email=$email")
        try {
            membersCollection(shopCode).document(userId).update("email", email).await()
            userShopsCollection(userId).document(shopCode).update("email", email).await()
        } catch (e: Exception) {
            Log.e(TAG, "updateMemberEmail failed", e)
        }
    }

    suspend fun removeMember(shopCode: String, userId: String) {
        Log.d(TAG, "removeMember: shopCode=$shopCode userId=$userId")
        try {
            membersCollection(shopCode).document(userId).delete().await()
            userShopsCollection(userId).document(shopCode).delete().await()
            shopsCollection().document(shopCode).update("memberIds", com.google.firebase.firestore.FieldValue.arrayRemove(userId)).await()
        } catch (e: Exception) {
            Log.e(TAG, "removeMember failed", e)
        }
    }

    suspend fun getShopMemberIds(shopCode: String): List<String> {
        return try {
            val doc = shopsCollection().document(shopCode).get().await()
            (doc.get("memberIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "getShopMemberIds failed", e)
            emptyList()
        }
    }

    suspend fun migrateShopMemberIds(shopCode: String): Int {
        Log.d(TAG, "migrateShopMemberIds: shopCode=$shopCode")
        var count = 0
        try {
            val shopDoc = shopsCollection().document(shopCode).get().await()
            var existingIds = (shopDoc.get("memberIds") as? List<*>)?.filterIsInstance<String>()?.toMutableList() ?: mutableListOf()
            val ownerId = shopDoc.getString("ownerId") ?: ""

            if (ownerId.isNotBlank()) {
                if (ownerId !in existingIds) {
                    existingIds.add(ownerId)
                }
                // Ensure owner has a member doc
                try {
                    val ownerDoc = membersCollection(shopCode).document(ownerId).get().await()
                    if (!ownerDoc.exists()) {
                        membersCollection(shopCode).document(ownerId).set(hashMapOf(
                            "role" to "owner",
                            "joinedAt" to (shopDoc.getLong("createdAt") ?: System.currentTimeMillis()),
                            "email" to ""
                        )).await()
                    }
                } catch (_: Exception) {}
            }

            // Try to discover additional members via collection group query
            try {
                val snapshot = db.collectionGroup(SHOPS)
                    .whereEqualTo(com.google.firebase.firestore.FieldPath.documentId(), shopCode)
                    .get()
                    .await()
                for (doc in snapshot.documents) {
                    val userId = doc.reference.parent.parent?.id ?: continue
                    if (userId !in existingIds) {
                        existingIds.add(userId)
                        // Also ensure member doc exists
                        val role = doc.getString("role") ?: "member"
                        val email = doc.getString("email") ?: ""
                        membersCollection(shopCode).document(userId).set(hashMapOf(
                            "role" to role,
                            "joinedAt" to (doc.getLong("joinedAt") ?: System.currentTimeMillis()),
                            "email" to email
                        )).await()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Collection group query failed (index may not exist): ${e.message}")
            }

            if (existingIds.isNotEmpty()) {
                shopsCollection().document(shopCode).update("memberIds", existingIds).await()
                count = existingIds.size
            }
            Log.d(TAG, "migrateShopMemberIds: $count member IDs synced")
        } catch (e: Exception) {
            Log.e(TAG, "migrateShopMemberIds failed", e)
        }
        return count
    }

    suspend fun transferOwnership(shopCode: String, currentOwnerId: String, newOwnerId: String) {
        Log.d(TAG, "transferOwnership: shopCode=$shopCode newOwnerId=$newOwnerId")
        try {
            shopsCollection().document(shopCode).update("ownerId", newOwnerId).await()
            updateMemberRole(shopCode, currentOwnerId, "admin")
            updateMemberRole(shopCode, newOwnerId, "owner")
        } catch (e: Exception) {
            Log.e(TAG, "transferOwnership failed", e)
        }
    }

    suspend fun getShopName(shopCode: String): String {
        return try {
            shopsCollection().document(shopCode).get().await().getString("name") ?: shopCode
        } catch (e: Exception) {
            shopCode
        }
    }

    suspend fun updateShopInfo(
        shopCode: String,
        name: String,
        address: String,
        phone: String,
        logo: String?,
        invoiceMessage: String
    ) {
        try {
            val updates = hashMapOf<String, Any>(
                "name" to name,
                "address" to address,
                "phone" to phone,
                "invoiceMessage" to invoiceMessage,
                "updatedAt" to System.currentTimeMillis()
            )
            if (logo != null) {
                updates["logo"] = logo
            } else {
                updates["logo"] = ""
            }
            shopsCollection().document(shopCode).update(updates as Map<String, Any>).await()
        } catch (e: Exception) {
            Log.e(TAG, "updateShopInfo failed", e)
        }
    }

    suspend fun getShopInfo(shopCode: String): Map<String, Any?> {
        return try {
            shopsCollection().document(shopCode).get().await().data ?: emptyMap()
        } catch (e: Exception) {
            Log.e(TAG, "getShopInfo failed", e)
            emptyMap()
        }
    }

    fun subscribeToCustomers(shopCode: String): Flow<List<Customer>> = callbackFlow {
        Log.d(TAG, "subscribeToCustomers: shopCode=$shopCode")
        val listener = customersCollection(shopCode)

            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "subscribeToCustomers error: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val customers = snapshot.documents.mapNotNull { doc ->
                        try {
                            Customer(
                                name = doc.getString("name") ?: "",
                                mobile = doc.getString("mobile") ?: doc.id,
                                totalBills = doc.getLong("totalBills")?.toInt() ?: 0,
                                totalSpent = doc.getDouble("totalSpent") ?: 0.0,
                                pendingAmount = doc.getDouble("pendingAmount") ?: 0.0,
                                creditAmount = doc.getDouble("creditAmount") ?: 0.0,
                                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                                updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis(),
                                deleted = doc.getBoolean("deleted") ?: false,
                                version = doc.getLong("version")?.toInt() ?: 1,
                                ownerId = doc.getString("ownerId") ?: ""
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(customers)
                }
            }
        listenerRegistrations["customers-$shopCode"] = listener
        awaitClose { listener.remove() }
    }

    fun subscribeToShopItems(shopCode: String): Flow<List<ShopItem>> = callbackFlow {
        Log.d(TAG, "subscribeToShopItems: shopCode=$shopCode")
        val listener = shopItemsCollection(shopCode)

            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "subscribeToShopItems error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { doc ->
                        try {
                            ShopItem(
                                id = doc.getString("id") ?: doc.id,
                                name = doc.getString("name") ?: "",
                                price = doc.getDouble("price") ?: 0.0,
                        category = doc.getString("category") ?: "",
                                barcode = doc.getString("barcode") ?: "",
                                stockQuantity = doc.getLong("stockQuantity")?.toInt() ?: 0,
                                lowStockThreshold = doc.getLong("lowStockThreshold")?.toInt() ?: 10,
                                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                                updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis(),
                                deleted = doc.getBoolean("deleted") ?: false,
                                version = doc.getLong("version")?.toInt() ?: 1,
                                ownerId = doc.getString("ownerId") ?: ""
                            )
                        } catch (e: Exception) { null }
                    }
                    trySend(items)
                }
            }
        listenerRegistrations["items-$shopCode"] = listener
        awaitClose { listener.remove() }
    }

    fun subscribeToBills(shopCode: String): Flow<List<Bill>> = callbackFlow {
        Log.d(TAG, "subscribeToBills: shopCode=$shopCode")
        val listener = billsCollection(shopCode)

            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "subscribeToBills error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val bills = snapshot.documents.mapNotNull { doc ->
                        try {
                            Bill(
                                id = doc.getString("id") ?: doc.id,
                                billNumber = doc.getString("billNumber") ?: "",
                                customerName = doc.getString("customerName") ?: "",
                                customerMobile = doc.getString("customerMobile") ?: "",
                                totalAmount = doc.getDouble("totalAmount") ?: 0.0,
                                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                                updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis(),
                                createdBy = doc.getString("createdBy") ?: "",
                                paymentStatus = doc.getString("paymentStatus") ?: "paid",
                                deleted = doc.getBoolean("deleted") ?: false,
                                version = doc.getLong("version")?.toInt() ?: 1,
                                ownerId = doc.getString("ownerId") ?: ""
                            )
                        } catch (e: Exception) { null }
                    }
                    trySend(bills)
                }
            }
        listenerRegistrations["bills-$shopCode"] = listener
        awaitClose { listener.remove() }
    }

    fun subscribeToBillItems(shopCode: String): Flow<List<BillItem>> = callbackFlow {
        Log.d(TAG, "subscribeToBillItems: shopCode=$shopCode")
        val listener = billItemsCollection(shopCode)

            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "subscribeToBillItems error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { doc ->
                        try {
                            BillItem(
                                id = doc.getString("id") ?: doc.id,
                                billId = doc.getString("billId") ?: "",
                                itemName = doc.getString("itemName") ?: "",
                                quantity = doc.getLong("quantity")?.toInt() ?: 0,
                                unitPrice = doc.getDouble("unitPrice") ?: 0.0,
                                subtotal = doc.getDouble("subtotal") ?: 0.0,
                                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                                updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis(),
                                deleted = doc.getBoolean("deleted") ?: false,
                                version = doc.getLong("version")?.toInt() ?: 1,
                                ownerId = doc.getString("ownerId") ?: ""
                            )
                        } catch (e: Exception) { null }
                    }
                    trySend(items)
                }
            }
        listenerRegistrations["billItems-$shopCode"] = listener
        awaitClose { listener.remove() }
    }

    fun subscribeToPayments(shopCode: String): Flow<List<CustomerPayment>> = callbackFlow {
        Log.d(TAG, "subscribeToPayments: shopCode=$shopCode")
        val listener = paymentsCollection(shopCode)

            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "subscribeToPayments error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val payments = snapshot.documents.mapNotNull { doc ->
                        try {
                            CustomerPayment(
                                uuid = doc.getString("uuid") ?: doc.id,
                                customerMobile = doc.getString("customerMobile") ?: "",
                                amount = doc.getDouble("amount") ?: 0.0,
                                note = doc.getString("note") ?: "",
                                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                                updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis(),
                                deleted = doc.getBoolean("deleted") ?: false,
                                version = doc.getLong("version")?.toInt() ?: 1,
                                ownerId = doc.getString("ownerId") ?: ""
                            )
                        } catch (e: Exception) { null }
                    }
                    trySend(payments)
                }
            }
        listenerRegistrations["payments-$shopCode"] = listener
        awaitClose { listener.remove() }
    }

    fun subscribeToUserRole(shopCode: String, userId: String): Flow<String?> = callbackFlow {
        Log.d(TAG, "subscribeToUserRole: shopCode=$shopCode userId=$userId")
        val listener = userShopsCollection(userId).document(shopCode)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "subscribeToUserRole error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    if (snapshot.exists()) {
                        trySend(snapshot.getString("role"))
                    } else {
                        trySend(null)
                    }
                }
            }
        listenerRegistrations["userRole-$shopCode"] = listener
        awaitClose { listener.remove() }
    }

    fun subscribeToShopInfo(shopCode: String): Flow<Map<String, Any?>> = callbackFlow {
        Log.d(TAG, "subscribeToShopInfo: shopCode=$shopCode")
        val listener = shopsCollection().document(shopCode)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "subscribeToShopInfo error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    trySend(snapshot.data ?: emptyMap())
                }
            }
        listenerRegistrations["shopInfo-$shopCode"] = listener
        awaitClose { listener.remove() }
    }

    fun unsubscribe(shopCode: String) {
        listOf("customers", "items", "bills", "billItems", "payments", "shopInfo", "userRole").forEach { prefix ->
            listenerRegistrations["$prefix-$shopCode"]?.remove()
            listenerRegistrations.remove("$prefix-$shopCode")
        }
    }

    fun unsubscribeAll() {
        listenerRegistrations.values.forEach { it.remove() }
        listenerRegistrations.clear()
    }
}