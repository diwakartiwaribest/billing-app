package com.shop.billing.data.repository

import com.shop.billing.data.local.dao.InvestmentDao
import com.shop.billing.data.local.entity.InvestmentEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InvestmentRepository @Inject constructor(
    private val investmentDao: InvestmentDao,
    private val productRepository: ProductRepository
) {
    fun observeAll(shopCode: String): Flow<List<InvestmentEntity>> = investmentDao.observeAll(shopCode)

    fun observeTotal(shopCode: String): Flow<Double> = investmentDao.observeTotal(shopCode)

    suspend fun recordProductPurchase(
        productId: String,
        productName: String,
        quantity: Int,
        purchasePrice: Double,
        sellingPriceAtPurchase: Double,
        barcode: String,
        shopCode: String
    ) {
        val now = System.currentTimeMillis()
        investmentDao.insert(
            InvestmentEntity(
                id = UUID.randomUUID().toString(),
                amount = purchasePrice * quantity,
                createdAt = now,
                shopCode = shopCode,
                productId = productId,
                productName = productName,
                quantity = quantity,
                purchasePrice = purchasePrice,
                sellingPriceAtPurchase = sellingPriceAtPurchase,
                barcode = barcode
            )
        )
        if (productId.isNotBlank()) {
            productRepository.increaseStock(productId, quantity)
        }
    }

    suspend fun deleteById(id: String) = investmentDao.deleteById(id)

    suspend fun getLatestPurchasePrice(productId: String, shopCode: String): Double? =
        investmentDao.getLatestPurchasePrice(productId, shopCode)

    suspend fun add(amount: Double, shopCode: String) {
        investmentDao.insert(
            InvestmentEntity(
                id = java.util.UUID.randomUUID().toString(),
                amount = amount,
                createdAt = System.currentTimeMillis(),
                shopCode = shopCode
            )
        )
    }
}
