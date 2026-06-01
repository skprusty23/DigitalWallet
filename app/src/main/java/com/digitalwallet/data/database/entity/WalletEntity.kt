package com.digitalwallet.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

enum class CurrencyType(val displayName: String, val symbol: String, val tokenSymbol: String) {
    USD("USD Token", "$", "USDT"),
    EUR("Euro Token", "€", "EURC"),
    SSD("SSD Token", "S", "SSDT")
}

enum class WalletStatus { ACTIVE, FROZEN, CLOSED }

@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val currencyType: CurrencyType,
    val balance: Double,
    val status: WalletStatus = WalletStatus.ACTIVE,
    val walletAddress: String,
    val ownerName: String = "Shridhar",
    val lastTransactionAt: LocalDateTime? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
