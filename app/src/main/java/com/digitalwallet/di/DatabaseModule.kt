package com.digitalwallet.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.digitalwallet.data.database.WalletDatabase
import com.digitalwallet.data.database.dao.*
import com.digitalwallet.security.SecurityManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dw_prefs")

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.dataStore

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context, securityManager: SecurityManager): WalletDatabase {
        val passphrase = securityManager.getOrCreateDatabaseKey()
        val factory = SupportFactory(passphrase)
        return Room.databaseBuilder(context, WalletDatabase::class.java, SecurityManager.DB_NAME)
            .openHelperFactory(factory)
            .build()
    }

    @Provides fun provideWalletDao(db: WalletDatabase): WalletDao = db.walletDao()
    @Provides fun provideTransactionDao(db: WalletDatabase): TransactionDao = db.transactionDao()
    @Provides fun provideContactDao(db: WalletDatabase): ContactDao = db.contactDao()
    @Provides fun provideSubscriptionDao(db: WalletDatabase): SubscriptionDao = db.subscriptionDao()
    @Provides fun provideRedemptionDao(db: WalletDatabase): RedemptionDao = db.redemptionDao()
    @Provides fun providePurchaseDao(db: WalletDatabase): PurchaseDao = db.purchaseDao()
    @Provides fun provideNotificationDao(db: WalletDatabase): NotificationDao = db.notificationDao()
}
