package com.digitalwallet.data.repository

import com.digitalwallet.data.database.dao.*
import com.digitalwallet.data.database.entity.*
import com.digitalwallet.domain.repository.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton class WalletRepositoryImpl @Inject constructor(private val dao: WalletDao) : WalletRepository {
    override fun getAllWallets() = dao.getAllWallets()
    override fun getTotalBalance() = dao.getTotalBalance()
    override suspend fun getById(id: Long) = dao.getById(id)
    override suspend fun insertWallet(wallet: WalletEntity) = dao.insert(wallet)
    override suspend fun updateWallet(wallet: WalletEntity) = dao.update(wallet)
    override suspend fun updateBalance(id: Long, delta: Double) = dao.updateBalance(id, delta)
    override suspend fun deleteWallet(wallet: WalletEntity) = dao.delete(wallet)
}

@Singleton class TransactionRepositoryImpl @Inject constructor(private val dao: TransactionDao) : TransactionRepository {
    override fun getAll() = dao.getAll()
    override fun getByWallet(walletId: Long) = dao.getByWallet(walletId)
    override fun getRecent(limit: Int) = dao.getRecent(limit)
    override fun getByCurrency(currency: CurrencyType) = dao.getByCurrency(currency)
    override fun getByType(type: TransactionType) = dao.getByType(type)
    override suspend fun getById(id: Long) = dao.getById(id)
    override suspend fun insert(tx: TransactionEntity) = dao.insert(tx)
}

@Singleton class ContactRepositoryImpl @Inject constructor(private val dao: ContactDao) : ContactRepository {
    override fun getAll() = dao.getAll()
    override fun getFavorites() = dao.getFavorites()
    override fun search(query: String) = dao.search(query)
    override suspend fun getById(id: Long) = dao.getById(id)
    override suspend fun getByAddress(address: String) = dao.getByAddress(address)
    override suspend fun insert(contact: ContactEntity) = dao.insert(contact)
    override suspend fun update(contact: ContactEntity) = dao.update(contact)
    override suspend fun delete(contact: ContactEntity) = dao.delete(contact)
}

@Singleton class SubscriptionRepositoryImpl @Inject constructor(private val dao: SubscriptionDao) : SubscriptionRepository {
    override fun getAll() = dao.getAll()
    override fun getActive() = dao.getActive()
    override suspend fun getById(id: Long) = dao.getById(id)
    override suspend fun insert(sub: SubscriptionEntity) = dao.insert(sub)
    override suspend fun update(sub: SubscriptionEntity) = dao.update(sub)
    override suspend fun delete(sub: SubscriptionEntity) = dao.delete(sub)
}

@Singleton class RedemptionRepositoryImpl @Inject constructor(private val dao: RedemptionDao) : RedemptionRepository {
    override fun getAll() = dao.getAll()
    override suspend fun getById(id: Long) = dao.getById(id)
    override suspend fun insert(r: RedemptionEntity) = dao.insert(r)
    override suspend fun update(r: RedemptionEntity) = dao.update(r)
}

@Singleton class PurchaseRepositoryImpl @Inject constructor(private val dao: PurchaseDao) : PurchaseRepository {
    override fun getAll() = dao.getAll()
    override fun getByWallet(walletId: Long) = dao.getByWallet(walletId)
    override suspend fun insert(p: PurchaseEntity) = dao.insert(p)
}

@Singleton class NotificationRepositoryImpl @Inject constructor(private val dao: NotificationDao) : NotificationRepository {
    override fun getAll() = dao.getAll()
    override fun getUnreadCount() = dao.getUnreadCount()
    override suspend fun markAllRead() = dao.markAllRead()
    override suspend fun insert(n: NotificationEntity) = dao.insert(n)
    override suspend fun delete(n: NotificationEntity) = dao.delete(n)
    override suspend fun deleteAll() = dao.deleteAll()
}
