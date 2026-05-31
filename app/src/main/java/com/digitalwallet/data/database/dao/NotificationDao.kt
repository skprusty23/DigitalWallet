package com.digitalwallet.data.database.dao

import androidx.room.*
import com.digitalwallet.data.database.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY createdAt DESC") fun getAll(): Flow<List<NotificationEntity>>
    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0") fun getUnreadCount(): Flow<Int>
    @Query("UPDATE notifications SET isRead = 1") suspend fun markAllRead()
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(n: NotificationEntity): Long
    @Delete suspend fun delete(n: NotificationEntity)
    @Query("DELETE FROM notifications") suspend fun deleteAll()
}
