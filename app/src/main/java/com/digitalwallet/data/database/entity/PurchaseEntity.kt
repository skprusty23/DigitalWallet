package com.digitalwallet.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

enum class PurchaseStatus { PENDING, COMPLETED, FAILED }

@Entity(tableName = "purchases")
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val walletId: Long,
    val walletName: String,
    val fiatAmount: Double,
    val tokenAmount: Double,
    val currencyType: CurrencyType,
    val exchangeRate: Double = 1.0,
    val status: PurchaseStatus = PurchaseStatus.COMPLETED,
    val purchasedAt: LocalDateTime = LocalDateTime.now()
)
