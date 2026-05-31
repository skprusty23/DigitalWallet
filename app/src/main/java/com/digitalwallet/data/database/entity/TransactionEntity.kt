package com.digitalwallet.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

enum class TransactionType { TRANSFER, RECEIVE, BUY, SELL, REDEEM, SUBSCRIPTION }
enum class TransactionStatus { PENDING, COMPLETED, FAILED, CANCELLED }

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val walletId: Long,
    val counterpartyName: String,
    val counterpartyAddress: String = "",
    val amount: Double,
    val currencyType: CurrencyType,
    val type: TransactionType,
    val status: TransactionStatus = TransactionStatus.COMPLETED,
    val description: String = "",
    val fee: Double = 0.0,
    val referenceId: String = "",
    val createdAt: LocalDateTime = LocalDateTime.now()
)
