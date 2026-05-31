package com.digitalwallet.data.database.dao

import androidx.room.*
import com.digitalwallet.data.database.entity.RedemptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RedemptionDao {
    @Query("SELECT * FROM redemptions ORDER BY requestedAt DESC") fun getAll(): Flow<List<RedemptionEntity>>
    @Query("SELECT * FROM redemptions WHERE id = :id") suspend fun getById(id: Long): RedemptionEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(r: RedemptionEntity): Long
    @Update suspend fun update(r: RedemptionEntity)
}
