package com.digitalwallet.domain.repository

import com.digitalwallet.data.database.entity.*
import kotlinx.coroutines.flow.Flow

interface WalletRepository {
    fun getAllWallets(): Flow<List<WalletEntity>>
    fun getTotalBalance(): Flow<Double?>
    suspend fun getById(id: Long): WalletEntity?
    suspend fun insertWallet(wallet: WalletEntity): Long
    suspend fun updateWallet(wallet: WalletEntity)
    suspend fun updateBalance(id: Long, delta: Double)
    suspend fun deleteWallet(wallet: WalletEntity)
}

interface TransactionRepository {
    fun getAll(): Flow<List<TransactionEntity>>
    fun getByWallet(walletId: Long): Flow<List<TransactionEntity>>
    fun getRecent(limit: Int = 10): Flow<List<TransactionEntity>>
    fun getByCurrency(currency: CurrencyType): Flow<List<TransactionEntity>>
    fun getByType(type: TransactionType): Flow<List<TransactionEntity>>
    suspend fun getById(id: Long): TransactionEntity?
    suspend fun insert(tx: TransactionEntity): Long
}

interface ContactRepository {
    fun getAll(): Flow<List<ContactEntity>>
    fun getFavorites(): Flow<List<ContactEntity>>
    fun search(query: String): Flow<List<ContactEntity>>
    suspend fun getById(id: Long): ContactEntity?
    suspend fun getByAddress(address: String): ContactEntity?
    suspend fun insert(contact: ContactEntity): Long
    suspend fun update(contact: ContactEntity)
    suspend fun delete(contact: ContactEntity)
}

interface SubscriptionRepository {
    fun getAll(): Flow<List<SubscriptionEntity>>
    fun getActive(): Flow<List<SubscriptionEntity>>
    suspend fun getById(id: Long): SubscriptionEntity?
    suspend fun insert(sub: SubscriptionEntity): Long
    suspend fun update(sub: SubscriptionEntity)
    suspend fun delete(sub: SubscriptionEntity)
}

interface RedemptionRepository {
    fun getAll(): Flow<List<RedemptionEntity>>
    suspend fun getById(id: Long): RedemptionEntity?
    suspend fun insert(r: RedemptionEntity): Long
    suspend fun update(r: RedemptionEntity)
}

interface PurchaseRepository {
    fun getAll(): Flow<List<PurchaseEntity>>
    fun getByWallet(walletId: Long): Flow<List<PurchaseEntity>>
    suspend fun insert(p: PurchaseEntity): Long
}

interface NotificationRepository {
    fun getAll(): Flow<List<NotificationEntity>>
    fun getUnreadCount(): Flow<Int>
    suspend fun markAllRead()
    suspend fun insert(n: NotificationEntity): Long
    suspend fun delete(n: NotificationEntity)
    suspend fun deleteAll()
}
