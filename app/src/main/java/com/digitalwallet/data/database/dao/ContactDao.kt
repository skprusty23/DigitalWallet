package com.digitalwallet.data.database.dao

import androidx.room.*
import com.digitalwallet.data.database.entity.ContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY isFavorite DESC, name ASC") fun getAll(): Flow<List<ContactEntity>>
    @Query("SELECT * FROM contacts WHERE isFavorite = 1 ORDER BY lastTransactionAt DESC") fun getFavorites(): Flow<List<ContactEntity>>
    @Query("SELECT * FROM contacts WHERE name LIKE '%' || :q || '%' OR email LIKE '%' || :q || '%'") fun search(q: String): Flow<List<ContactEntity>>
    @Query("SELECT * FROM contacts WHERE id = :id") suspend fun getById(id: Long): ContactEntity?
    @Query("SELECT * FROM contacts WHERE walletAddress = :address LIMIT 1") suspend fun getByAddress(address: String): ContactEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(c: ContactEntity): Long
    @Update suspend fun update(c: ContactEntity)
    @Delete suspend fun delete(c: ContactEntity)
}
