package com.digitalwallet.data.database.dao

import androidx.room.*
import com.digitalwallet.data.database.entity.PurchaseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseDao {
    @Query("SELECT * FROM purchases ORDER BY purchasedAt DESC") fun getAll(): Flow<List<PurchaseEntity>>
    @Query("SELECT * FROM purchases WHERE walletId = :walletId ORDER BY purchasedAt DESC") fun getByWallet(walletId: Long): Flow<List<PurchaseEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(p: PurchaseEntity): Long
}
