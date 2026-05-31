package com.digitalwallet.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.digitalwallet.data.database.converter.Converters
import com.digitalwallet.data.database.dao.*
import com.digitalwallet.data.database.entity.*

@Database(
    entities = [
        WalletEntity::class,
        TransactionEntity::class,
        ContactEntity::class,
        SubscriptionEntity::class,
        RedemptionEntity::class,
        PurchaseEntity::class,
        NotificationEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class WalletDatabase : RoomDatabase() {
    abstract fun walletDao(): WalletDao
    abstract fun transactionDao(): TransactionDao
    abstract fun contactDao(): ContactDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun redemptionDao(): RedemptionDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun notificationDao(): NotificationDao
}
