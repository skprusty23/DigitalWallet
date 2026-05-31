package com.digitalwallet.data.database.dao

import androidx.room.*
import com.digitalwallet.data.database.entity.SubscriptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions ORDER BY createdAt DESC") fun getAll(): Flow<List<SubscriptionEntity>>
    @Query("SELECT * FROM subscriptions WHERE isActive = 1 ORDER BY nextRenewalDate ASC") fun getActive(): Flow<List<SubscriptionEntity>>
    @Query("SELECT * FROM subscriptions WHERE id = :id") suspend fun getById(id: Long): SubscriptionEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(s: SubscriptionEntity): Long
    @Update suspend fun update(s: SubscriptionEntity)
    @Delete suspend fun delete(s: SubscriptionEntity)
}
