package com.shop.billing.data.repository

import com.shop.billing.data.local.dao.InvestmentDao
import com.shop.billing.data.local.entity.InvestmentEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InvestmentRepository @Inject constructor(
    private val investmentDao: InvestmentDao
) {
    fun observeAll(shopCode: String): Flow<List<InvestmentEntity>> = investmentDao.observeAll(shopCode)

    fun observeTotal(shopCode: String): Flow<Double> = investmentDao.observeTotal(shopCode)

    suspend fun add(amount: Double, shopCode: String) {
        investmentDao.insert(
            InvestmentEntity(
                id = UUID.randomUUID().toString(),
                amount = amount,
                createdAt = System.currentTimeMillis(),
                shopCode = shopCode
            )
        )
    }

    suspend fun deleteById(id: String) = investmentDao.deleteById(id)
}
