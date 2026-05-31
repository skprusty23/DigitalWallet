package com.digitalwallet.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

enum class SubscriptionPlan(val displayName: String) {
    BASIC("Basic Plan"),
    STANDARD("Standard Plan"),
    PREMIUM("Premium Plan"),
    ENTERPRISE("Enterprise Plan")
}

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val walletId: Long,
    val plan: SubscriptionPlan,
    val tokenAmount: Double,
    val currencyType: CurrencyType,
    val isActive: Boolean = true,
    val autoRenew: Boolean = true,
    val startDate: LocalDate = LocalDate.now(),
    val nextRenewalDate: LocalDate,
    val description: String = "",
    val createdAt: LocalDateTime = LocalDateTime.now()
)
