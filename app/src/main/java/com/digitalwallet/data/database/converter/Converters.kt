package com.digitalwallet.data.database.converter

import androidx.room.TypeConverter
import com.digitalwallet.data.database.entity.*
import java.time.LocalDate
import java.time.LocalDateTime

class Converters {
    @TypeConverter fun fromLocalDateTime(v: LocalDateTime?): String? = v?.toString()
    @TypeConverter fun toLocalDateTime(v: String?): LocalDateTime? = v?.let { LocalDateTime.parse(it) }
    @TypeConverter fun fromLocalDate(v: LocalDate?): String? = v?.toString()
    @TypeConverter fun toLocalDate(v: String?): LocalDate? = v?.let { LocalDate.parse(it) }
    @TypeConverter fun fromCurrencyType(v: CurrencyType): String = v.name
    @TypeConverter fun toCurrencyType(v: String): CurrencyType = CurrencyType.valueOf(v)
    @TypeConverter fun fromWalletStatus(v: WalletStatus): String = v.name
    @TypeConverter fun toWalletStatus(v: String): WalletStatus = WalletStatus.valueOf(v)
    @TypeConverter fun fromTransactionType(v: TransactionType): String = v.name
    @TypeConverter fun toTransactionType(v: String): TransactionType = TransactionType.valueOf(v)
    @TypeConverter fun fromTransactionStatus(v: TransactionStatus): String = v.name
    @TypeConverter fun toTransactionStatus(v: String): TransactionStatus = TransactionStatus.valueOf(v)
    @TypeConverter fun fromRedemptionStatus(v: RedemptionStatus): String = v.name
    @TypeConverter fun toRedemptionStatus(v: String): RedemptionStatus = RedemptionStatus.valueOf(v)
    @TypeConverter fun fromPurchaseStatus(v: PurchaseStatus): String = v.name
    @TypeConverter fun toPurchaseStatus(v: String): PurchaseStatus = PurchaseStatus.valueOf(v)
    @TypeConverter fun fromSubscriptionPlan(v: SubscriptionPlan): String = v.name
    @TypeConverter fun toSubscriptionPlan(v: String): SubscriptionPlan = SubscriptionPlan.valueOf(v)
    @TypeConverter fun fromNotificationType(v: NotificationType): String = v.name
    @TypeConverter fun toNotificationType(v: String): NotificationType = NotificationType.valueOf(v)
}
