package com.digitalwallet.data.database.dao

import androidx.room.*
import com.digitalwallet.data.database.entity.CurrencyType
import com.digitalwallet.data.database.entity.TransactionEntity
import com.digitalwallet.data.database.entity.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY createdAt DESC") fun getAll(): Flow<List<TransactionEntity>>
    @Query("SELECT * FROM transactions WHERE walletId = :walletId ORDER BY createdAt DESC") fun getByWallet(walletId: Long): Flow<List<TransactionEntity>>
    @Query("SELECT * FROM transactions ORDER BY createdAt DESC LIMIT :limit") fun getRecent(limit: Int = 10): Flow<List<TransactionEntity>>
    @Query("SELECT * FROM transactions WHERE id = :id") suspend fun getById(id: Long): TransactionEntity?
    @Query("SELECT * FROM transactions WHERE currencyType = :currency ORDER BY createdAt DESC") fun getByCurrency(currency: CurrencyType): Flow<List<TransactionEntity>>
    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY createdAt DESC") fun getByType(type: TransactionType): Flow<List<TransactionEntity>>
    @Query("SELECT SUM(amount) FROM transactions WHERE walletId = :walletId AND type IN ('RECEIVE','BUY')") suspend fun getTotalIn(walletId: Long): Double?
    @Query("SELECT SUM(amount) FROM transactions WHERE walletId = :walletId AND type IN ('TRANSFER','SELL','REDEEM','SUBSCRIPTION')") suspend fun getTotalOut(walletId: Long): Double?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(tx: TransactionEntity): Long
    @Delete suspend fun delete(tx: TransactionEntity)
}
