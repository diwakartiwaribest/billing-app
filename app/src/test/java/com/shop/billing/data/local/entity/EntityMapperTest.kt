package com.shop.billing.data.local.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class EntityMapperTest {

    @Test
    fun `InvoiceEntity toBill maps all fields`() {
        val now = Instant.now()
        val entity = InvoiceEntity(
            id = "bill-1",
            billNumber = "INV-001",
            customerName = "John",
            customerMobile = "9876543210",
            totalAmount = 500.0,
            paymentStatus = "credit",
            createdBy = "owner",
            shopCode = "SHOP1",
            createdAt = now,
            updatedAt = now,
            deleted = false,
            version = 3,
            ownerId = "owner-1",
            syncStatus = SyncStatus.SYNCED
        )
        val bill = entity.toBill()
        assertEquals("bill-1", bill.id)
        assertEquals("INV-001", bill.billNumber)
        assertEquals("John", bill.customerName)
        assertEquals("9876543210", bill.customerMobile)
        assertEquals(500.0, bill.totalAmount, 0.001)
        assertEquals("credit", bill.paymentStatus)
        assertEquals("owner", bill.createdBy)
        assertEquals(now.toEpochMilli(), bill.createdAt)
        assertEquals(false, bill.deleted)
        assertEquals(3, bill.version)
    }

    @Test
    fun `InvoiceItemEntity toBillItem maps all fields`() {
        val now = Instant.now()
        val entity = InvoiceItemEntity(
            id = "item-1",
            invoiceId = "bill-1",
            itemName = "Product A",
            quantity = 2,
            unitPrice = 250.0,
            subtotal = 500.0,
            shopCode = "SHOP1",
            createdAt = now,
            updatedAt = now,
            deleted = true,
            version = 2,
            ownerId = "owner-1",
            syncStatus = SyncStatus.PENDING_DELETE
        )
        val item = entity.toBillItem()
        assertEquals("item-1", item.id)
        assertEquals("bill-1", item.billId)
        assertEquals("Product A", item.itemName)
        assertEquals(2, item.quantity)
        assertEquals(250.0, item.unitPrice, 0.001)
        assertEquals(500.0, item.subtotal, 0.001)
        assertEquals(now.toEpochMilli(), item.createdAt)
        assertTrue(item.deleted)
        assertEquals(2, item.version)
    }

    @Test
    fun `ProductEntity toShopItem maps all fields`() {
        val now = Instant.now()
        val entity = ProductEntity(
            id = "prod-1",
            name = "Widget",
            price = 99.99,
            category = "Electronics",
            shopCode = "SHOP1",
            createdAt = now,
            updatedAt = now,
            deleted = false,
            version = 1,
            ownerId = "",
            syncStatus = SyncStatus.SYNCED
        )
        val item = entity.toShopItem()
        assertEquals("prod-1", item.id)
        assertEquals("Widget", item.name)
        assertEquals(99.99, item.price, 0.001)
        assertEquals("Electronics", item.category)
        assertEquals(now.toEpochMilli(), item.createdAt)
        assertEquals(false, item.deleted)
        assertEquals(1, item.version)
    }

    @Test
    fun `CustomerPaymentEntity toCustomerPayment maps all fields`() {
        val now = Instant.now()
        val entity = CustomerPaymentEntity(
            uuid = "pay-1",
            customerMobile = "9876543210",
            amount = 1000.0,
            note = "Advance payment",
            shopCode = "SHOP1",
            createdAt = now,
            updatedAt = now,
            deleted = false,
            version = 1,
            ownerId = "",
            syncStatus = SyncStatus.PENDING_CREATE
        )
        val payment = entity.toCustomerPayment()
        assertEquals("pay-1", payment.uuid)
        assertEquals("9876543210", payment.customerMobile)
        assertEquals(1000.0, payment.amount, 0.001)
        assertEquals("Advance payment", payment.note)
        assertEquals(now.toEpochMilli(), payment.createdAt)
        assertEquals(false, payment.deleted)
        assertEquals(1, payment.version)
    }
}
