package com.shop.billing.data.sync

enum class OperationType { CREATE, UPDATE, DELETE }
enum class EntityType { PRODUCT, CUSTOMER, INVOICE, INVOICE_ITEM, CUSTOMER_PAYMENT }

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Synced(val lastSyncTime: Long = System.currentTimeMillis()) : SyncState()
    data class Error(val message: String) : SyncState()
}
