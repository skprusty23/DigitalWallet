package com.digitalwallet.data.database.dao

import androidx.room.*
import com.digitalwallet.data.database.entity.CurrencyType
import com.digitalwallet.data.database.entity.WalletEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallets ORDER BY createdAt ASC") fun getAllWallets(): Flow<List<WalletEntity>>
    @Query("SELECT * FROM wallets WHERE id = :id") suspend fun getById(id: Long): WalletEntity?
    @Query("SELECT * FROM wallets WHERE currencyType = :type") suspend fun getByCurrency(type: CurrencyType): WalletEntity?
    @Query("SELECT SUM(balance) FROM wallets WHERE status = 'ACTIVE'") fun getTotalBalance(): Flow<Double?>
    @Query("UPDATE wallets SET balance = balance + :delta, updatedAt = :now WHERE id = :id")
    suspend fun updateBalance(id: Long, delta: Double, now: String = java.time.LocalDateTime.now().toString())
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(wallet: WalletEntity): Long
    @Update suspend fun update(wallet: WalletEntity)
    @Delete suspend fun delete(wallet: WalletEntity)
}
