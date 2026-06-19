package com.shop.billing.data.sync

import android.content.Context
import com.shop.billing.data.local.dao.CustomerDao
import com.shop.billing.data.local.dao.CustomerPaymentDao
import com.shop.billing.data.local.dao.InvoiceDao
import com.shop.billing.data.local.dao.InvoiceItemDao
import com.shop.billing.data.local.dao.ProductDao
import com.shop.billing.data.local.entity.CustomerEntity
import com.shop.billing.data.local.entity.CustomerPaymentEntity
import com.shop.billing.data.local.entity.InvoiceEntity
import com.shop.billing.data.local.entity.InvoiceItemEntity
import com.shop.billing.data.local.entity.ProductEntity
import com.shop.billing.data.local.entity.SyncStatus
import com.shop.billing.data.remote.SupabaseClient
import com.shop.billing.data.remote.SupabaseRealtimeClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SyncEngineMergeTest {

    private val productDao = mockk<ProductDao>(relaxed = true)
    private val customerDao = mockk<CustomerDao>(relaxed = true)
    private val invoiceDao = mockk<InvoiceDao>(relaxed = true)
    private val invoiceItemDao = mockk<InvoiceItemDao>(relaxed = true)
    private val paymentDao = mockk<CustomerPaymentDao>(relaxed = true)
    private val supabaseClient = mockk<SupabaseClient>(relaxed = true)
    private val realtimeClient = mockk<SupabaseRealtimeClient>(relaxed = true) {
        coEvery { events } returns MutableStateFlow(com.shop.billing.data.remote.RealtimeChange("", "", null, null))
    }
    private val context = mockk<Context>(relaxed = true)

    private val syncEngine = SyncEngine(
        supabaseClient, productDao, customerDao, invoiceDao, invoiceItemDao, paymentDao,
        realtimeClient, context
    )

    @Test
    fun `local pending_delete invoice is NOT overwritten by remote pull`() = runTest {
        val shopCode = "SHOP1"
        val invoiceId = "bill-pending-delete"

        val localInvoice = InvoiceEntity(
            id = invoiceId, billNumber = "INV-001",
            deleted = true, syncStatus = SyncStatus.PENDING_DELETE, shopCode = shopCode
        )

        coEvery { invoiceDao.getAllIncludeDeleted(shopCode) } returns listOf(localInvoice)
        coEvery { invoiceDao.getPendingSync(shopCode) } returns emptyList()
        coEvery { customerDao.getPendingSync(shopCode) } returns emptyList()
        coEvery { productDao.getPendingSync(shopCode) } returns emptyList()
        coEvery { paymentDao.getPendingSync(shopCode) } returns emptyList()

        val remoteBill = com.shop.billing.data.model.Bill(
            id = invoiceId, billNumber = "INV-001", deleted = true
        )
        coEvery { supabaseClient.pullBills(any(), any(), shopCode, any()) } returns Pair(
            listOf(remoteBill), emptyList()
        )
        coEvery { supabaseClient.pullShopItems(any(), any(), shopCode, any()) } returns emptyList()
        coEvery { supabaseClient.pullCustomers(any(), any(), shopCode, any()) } returns emptyList()
        coEvery { supabaseClient.pullCustomerPayments(any(), any(), shopCode, any()) } returns emptyList()

        syncEngine.fullSyncInternal("url", "key", shopCode)

        coVerify(inverse = true) { invoiceDao.upsert(any<InvoiceEntity>()) }
    }

    @Test
    fun `local pending_create invoice is NOT overwritten by remote`() = runTest {
        val shopCode = "SHOP1"
        val invoiceId = "bill-pending-create"

        val localInvoice = InvoiceEntity(
            id = invoiceId, billNumber = "INV-002",
            deleted = false, syncStatus = SyncStatus.PENDING_CREATE, shopCode = shopCode
        )

        coEvery { invoiceDao.getAllIncludeDeleted(shopCode) } returns listOf(localInvoice)
        coEvery { invoiceDao.getPendingSync(shopCode) } returns emptyList()
        coEvery { customerDao.getPendingSync(shopCode) } returns emptyList()
        coEvery { productDao.getPendingSync(shopCode) } returns emptyList()
        coEvery { paymentDao.getPendingSync(shopCode) } returns emptyList()

        val remoteBill = com.shop.billing.data.model.Bill(
            id = invoiceId, billNumber = "INV-002-REMOTE", deleted = false
        )
        coEvery { supabaseClient.pullBills(any(), any(), shopCode, any()) } returns Pair(
            listOf(remoteBill), emptyList()
        )
        coEvery { supabaseClient.pullShopItems(any(), any(), shopCode, any()) } returns emptyList()
        coEvery { supabaseClient.pullCustomers(any(), any(), shopCode, any()) } returns emptyList()
        coEvery { supabaseClient.pullCustomerPayments(any(), any(), shopCode, any()) } returns emptyList()

        syncEngine.fullSyncInternal("url", "key", shopCode)

        coVerify(inverse = true) { invoiceDao.upsert(any<InvoiceEntity>()) }
    }

    @Test
    fun `synced local invoice IS overwritten by remote`() = runTest {
        val shopCode = "SHOP1"
        val invoiceId = "bill-synced"

        val localInvoice = InvoiceEntity(
            id = invoiceId, billNumber = "INV-003", totalAmount = 100.0,
            deleted = false, syncStatus = SyncStatus.SYNCED, shopCode = shopCode,
            createdAt = java.time.Instant.now(), updatedAt = java.time.Instant.now()
        )

        coEvery { invoiceDao.getAllIncludeDeleted(shopCode) } returns listOf(localInvoice)
        coEvery { invoiceDao.getPendingSync(shopCode) } returns emptyList()
        coEvery { customerDao.getPendingSync(shopCode) } returns emptyList()
        coEvery { productDao.getPendingSync(shopCode) } returns emptyList()
        coEvery { paymentDao.getPendingSync(shopCode) } returns emptyList()

        val remoteBill = com.shop.billing.data.model.Bill(
            id = invoiceId, billNumber = "INV-003", totalAmount = 200.0, deleted = false
        )
        coEvery { supabaseClient.pullBills(any(), any(), shopCode, any()) } returns Pair(
            listOf(remoteBill), emptyList()
        )
        coEvery { supabaseClient.pullShopItems(any(), any(), shopCode, any()) } returns emptyList()
        coEvery { supabaseClient.pullCustomers(any(), any(), shopCode, any()) } returns emptyList()
        coEvery { supabaseClient.pullCustomerPayments(any(), any(), shopCode, any()) } returns emptyList()

        syncEngine.fullSyncInternal("url", "key", shopCode)

        coVerify { invoiceDao.upsert(any<InvoiceEntity>()) }
    }

    @Test
    fun `local pending_delete product is NOT overwritten by remote`() = runTest {
        val shopCode = "SHOP1"
        val productId = "prod-pending-delete"

        val localProduct = ProductEntity(
            id = productId, name = "Old Name", price = 0.0,
            deleted = true, syncStatus = SyncStatus.PENDING_DELETE, shopCode = shopCode
        )

        coEvery { productDao.getAllIncludeDeleted(shopCode) } returns listOf(localProduct)
        coEvery { productDao.getPendingSync(shopCode) } returns emptyList()
        coEvery { customerDao.getPendingSync(shopCode) } returns emptyList()
        coEvery { invoiceDao.getPendingSync(shopCode) } returns emptyList()
        coEvery { paymentDao.getPendingSync(shopCode) } returns emptyList()

        val remoteProduct = com.shop.billing.data.model.ShopItem(
            id = productId, name = "New Name (remote)", price = 50.0,
            category = "General", deleted = false, version = 1
        )
        coEvery { supabaseClient.pullShopItems(any(), any(), shopCode, any()) } returns listOf(remoteProduct)
        coEvery { supabaseClient.pullBills(any(), any(), shopCode, any()) } returns Pair(emptyList(), emptyList())
        coEvery { supabaseClient.pullCustomers(any(), any(), shopCode, any()) } returns emptyList()
        coEvery { supabaseClient.pullCustomerPayments(any(), any(), shopCode, any()) } returns emptyList()

        syncEngine.fullSyncInternal("url", "key", shopCode)

        coVerify(inverse = true) { productDao.upsert(any<ProductEntity>()) }
    }

    @Test
    fun `local pending_delete customer payment is NOT overwritten`() = runTest {
        val shopCode = "SHOP1"
        val paymentUuid = "pay-pending-delete"

        val localPayment = CustomerPaymentEntity(
            uuid = paymentUuid, customerMobile = "9876543210", amount = 500.0,
            deleted = true, syncStatus = SyncStatus.PENDING_DELETE, shopCode = shopCode
        )

        coEvery { paymentDao.getAllIncludeDeleted(shopCode) } returns listOf(localPayment)
        coEvery { paymentDao.getPendingSync(shopCode) } returns emptyList()
        coEvery { customerDao.getPendingSync(shopCode) } returns emptyList()
        coEvery { invoiceDao.getPendingSync(shopCode) } returns emptyList()
        coEvery { productDao.getPendingSync(shopCode) } returns emptyList()

        val remotePayment = com.shop.billing.data.model.CustomerPayment(
            uuid = paymentUuid, customerMobile = "9876543210", amount = 500.0, deleted = false
        )
        coEvery { supabaseClient.pullCustomerPayments(any(), any(), shopCode, any()) } returns listOf(remotePayment)
        coEvery { supabaseClient.pullShopItems(any(), any(), shopCode, any()) } returns emptyList()
        coEvery { supabaseClient.pullCustomers(any(), any(), shopCode, any()) } returns emptyList()
        coEvery { supabaseClient.pullBills(any(), any(), shopCode, any()) } returns Pair(emptyList(), emptyList())

        syncEngine.fullSyncInternal("url", "key", shopCode)

        coVerify(inverse = true) { paymentDao.upsert(any<CustomerPaymentEntity>()) }
    }
}
