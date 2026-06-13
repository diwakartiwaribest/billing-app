package com.shop.billing.data.repository

import com.shop.billing.data.local.dao.BillDao
import com.shop.billing.data.local.dao.BillItemDao
import com.shop.billing.data.local.entity.BillEntity
import com.shop.billing.data.local.entity.BillItemEntity
import com.shop.billing.data.model.Bill
import com.shop.billing.data.model.BillItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillRepository @Inject constructor(
    private val billDao: BillDao,
    private val billItemDao: BillItemDao
) {
    fun getAllBills(): Flow<List<Bill>> {
        return billDao.getAllBills().map { entities ->
            entities.map { it.toModel() }
        }
    }

    suspend fun getBillById(id: String): Bill? {
        return billDao.getBillById(id)?.toModel()
    }

    suspend fun getItemsByBillId(billId: String): List<BillItem> {
        return billItemDao.getItemsByBillId(billId).map { it.toModel() }
    }

    suspend fun getBillsByCustomerMobile(mobile: String): List<Bill> {
        return billDao.getBillsByCustomerMobile(mobile).map { it.toModel() }
    }

    suspend fun getMaxBillNumber(): String? {
        return billDao.getMaxBillNumber()
    }

    suspend fun insertBill(bill: Bill) {
        billDao.insertBill(bill.toEntity())
    }

    suspend fun insertBillWithItems(bill: Bill, items: List<BillItem>) {
        billDao.insertBill(bill.toEntity())
        billItemDao.insertItems(items.map { it.toEntity() })
    }

    suspend fun insertBills(bills: List<Bill>) {
        billDao.insertBills(bills.map { it.toEntity() })
    }

    suspend fun insertBillItems(items: List<BillItem>) {
        billItemDao.insertItems(items.map { it.toEntity() })
    }

    suspend fun deleteBill(id: String) {
        billDao.deleteBill(id)
        billItemDao.deleteItemsByBillId(id)
    }

    suspend fun deleteAll() {
        billDao.deleteAll()
        billItemDao.deleteAll()
    }
}

private fun BillEntity.toModel() = Bill(
    id = id,
    billNumber = billNumber,
    customerName = customerName,
    customerMobile = customerMobile,
    totalAmount = totalAmount,
    createdAt = createdAt,
    createdBy = createdBy,
    paymentStatus = paymentStatus
)

private fun Bill.toEntity() = BillEntity(
    id = id,
    billNumber = billNumber,
    customerName = customerName,
    customerMobile = customerMobile,
    totalAmount = totalAmount,
    createdAt = createdAt,
    createdBy = createdBy,
    paymentStatus = paymentStatus
)

private fun BillItemEntity.toModel() = BillItem(
    id = id,
    billId = billId,
    itemName = itemName,
    quantity = quantity,
    unitPrice = unitPrice,
    subtotal = subtotal
)

private fun BillItem.toEntity() = BillItemEntity(
    id = id,
    billId = billId,
    itemName = itemName,
    quantity = quantity,
    unitPrice = unitPrice,
    subtotal = subtotal
)
