package com.digitalwallet.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

enum class RedemptionStatus { PENDING, PROCESSING, COMPLETED, REJECTED }

@Entity(tableName = "redemptions")
data class RedemptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val walletId: Long,
    val walletName: String,
    val tokenAmount: Double,
    val currencyType: CurrencyType,
    val status: RedemptionStatus = RedemptionStatus.PENDING,
    val notes: String = "",
    val requestedAt: LocalDateTime = LocalDateTime.now(),
    val processedAt: LocalDateTime? = null
)
