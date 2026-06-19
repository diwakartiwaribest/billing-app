package com.shop.billing.data.local.entity

import com.shop.billing.data.model.Bill
import com.shop.billing.data.model.BillItem
import com.shop.billing.data.model.Customer
import com.shop.billing.data.model.CustomerPayment
import com.shop.billing.data.model.ShopItem
import java.time.Instant

fun Customer.toEntity(shopCode: String): CustomerEntity = CustomerEntity(
    mobile = mobile,
    name = name,
    totalBills = totalBills,
    totalSpent = totalSpent,
    pendingAmount = pendingAmount,
    creditAmount = creditAmount,
    shopCode = shopCode,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
    deleted = deleted,
    version = version,
    ownerId = ownerId,
    syncStatus = SyncStatus.SYNCED
)

fun ShopItem.toEntity(shopCode: String): ProductEntity = ProductEntity(
    id = id,
    name = name,
    price = price,
    category = category,
    shopCode = shopCode,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
    deleted = deleted,
    version = version,
    ownerId = ownerId,
    syncStatus = SyncStatus.SYNCED
)

fun Bill.toEntity(shopCode: String): InvoiceEntity = InvoiceEntity(
    id = id,
    billNumber = billNumber,
    customerName = customerName,
    customerMobile = customerMobile,
    totalAmount = totalAmount,
    paymentStatus = paymentStatus,
    createdBy = createdBy,
    shopCode = shopCode,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
    deleted = deleted,
    version = version,
    ownerId = ownerId,
    syncStatus = SyncStatus.SYNCED
)

fun BillItem.toEntity(shopCode: String): InvoiceItemEntity = InvoiceItemEntity(
    id = id,
    invoiceId = billId,
    itemName = itemName,
    quantity = quantity,
    unitPrice = unitPrice,
    subtotal = subtotal,
    shopCode = shopCode,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
    deleted = deleted,
    version = version,
    ownerId = ownerId,
    syncStatus = SyncStatus.SYNCED
)

fun CustomerPayment.toEntity(shopCode: String): CustomerPaymentEntity = CustomerPaymentEntity(
    uuid = uuid,
    customerMobile = customerMobile,
    amount = amount,
    note = note,
    shopCode = shopCode,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
    deleted = deleted,
    version = version,
    ownerId = ownerId,
    syncStatus = SyncStatus.SYNCED
)
