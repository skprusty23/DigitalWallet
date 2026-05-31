package com.digitalwallet.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val email: String = "",
    val walletAddress: String,
    val preferredCurrency: CurrencyType = CurrencyType.USD,
    val isFavorite: Boolean = false,
    val avatarColor: Long = 0xFF1565C0,
    val lastTransactionAt: LocalDateTime? = null,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
