package com.digitalwallet.di

import com.digitalwallet.data.repository.*
import com.digitalwallet.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds @Singleton abstract fun bindWalletRepo(impl: WalletRepositoryImpl): WalletRepository
    @Binds @Singleton abstract fun bindTransactionRepo(impl: TransactionRepositoryImpl): TransactionRepository
    @Binds @Singleton abstract fun bindContactRepo(impl: ContactRepositoryImpl): ContactRepository
    @Binds @Singleton abstract fun bindSubscriptionRepo(impl: SubscriptionRepositoryImpl): SubscriptionRepository
    @Binds @Singleton abstract fun bindRedemptionRepo(impl: RedemptionRepositoryImpl): RedemptionRepository
    @Binds @Singleton abstract fun bindPurchaseRepo(impl: PurchaseRepositoryImpl): PurchaseRepository
    @Binds @Singleton abstract fun bindNotificationRepo(impl: NotificationRepositoryImpl): NotificationRepository
}
