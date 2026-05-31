package com.digitalwallet.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.digitalwallet.ui.screens.auth.AuthScreen
import com.digitalwallet.ui.screens.auth.SetupPinScreen
import com.digitalwallet.ui.screens.buy.BuyTokensScreen
import com.digitalwallet.ui.screens.contacts.FrequentContactsScreen
import com.digitalwallet.ui.screens.dashboard.DashboardScreen
import com.digitalwallet.ui.screens.marketplace.MarketplaceScreen
import com.digitalwallet.ui.screens.notifications.NotificationsScreen
import com.digitalwallet.ui.screens.receive.QRScanScreen
import com.digitalwallet.ui.screens.receive.ReceiveScreen
import com.digitalwallet.ui.screens.redeem.RedeemTokensScreen
import com.digitalwallet.ui.screens.reports.ReportsScreen
import com.digitalwallet.ui.screens.sell.SellTokensScreen
import com.digitalwallet.ui.screens.send.SendConfirmScreen
import com.digitalwallet.ui.screens.send.SendTokensScreen
import com.digitalwallet.ui.screens.settings.SettingsScreen
import com.digitalwallet.ui.screens.splash.SplashScreen
import com.digitalwallet.ui.screens.subscriptions.SubscriptionsScreen
import com.digitalwallet.ui.screens.transactions.TransactionDetailScreen
import com.digitalwallet.ui.screens.transactions.TransactionsScreen
import com.digitalwallet.ui.screens.wallet.AddWalletScreen
import com.digitalwallet.ui.screens.wallet.WalletDetailScreen
import com.digitalwallet.ui.screens.wallet.WalletsScreen

@Composable
fun WalletNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = NavRoutes.SPLASH) {

        composable(NavRoutes.SPLASH) {
            SplashScreen(
                onNavigateToAuth = {
                    navController.navigate(NavRoutes.AUTH) {
                        popUpTo(NavRoutes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToSetupPin = {
                    navController.navigate(NavRoutes.SETUP_PIN) {
                        popUpTo(NavRoutes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.AUTH) {
            AuthScreen(
                onAuthSuccess = {
                    navController.navigate(NavRoutes.DASHBOARD) {
                        popUpTo(NavRoutes.AUTH) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.SETUP_PIN) {
            SetupPinScreen(
                onPinSet = {
                    navController.navigate(NavRoutes.DASHBOARD) {
                        popUpTo(NavRoutes.SETUP_PIN) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.DASHBOARD) {
            DashboardScreen(
                onNavigateToWallets = { navController.navigate(NavRoutes.WALLETS) },
                onNavigateToTransactions = { navController.navigate(NavRoutes.TRANSACTIONS) },
                onNavigateToReports = { navController.navigate(NavRoutes.REPORTS) },
                onNavigateToSettings = { navController.navigate(NavRoutes.SETTINGS) },
                onNavigateToSend = { navController.navigate(NavRoutes.SEND) },
                onNavigateToReceive = { navController.navigate(NavRoutes.RECEIVE.replace("{walletId}", "0")) },
                onNavigateToBuy = { navController.navigate(NavRoutes.BUY) },
                onNavigateToSell = { navController.navigate(NavRoutes.SELL) },
                onNavigateToQrScan = { navController.navigate(NavRoutes.QR_SCAN) },
                onNavigateToNotifications = { navController.navigate(NavRoutes.NOTIFICATIONS) },
                onWalletClick = { id -> navController.navigate(NavRoutes.walletDetail(id)) },
                onTransactionClick = { id -> navController.navigate(NavRoutes.transactionDetail(id)) }
            )
        }

        composable(NavRoutes.WALLETS) {
            WalletsScreen(
                onBack = { navController.popBackStack() },
                onWalletClick = { id -> navController.navigate(NavRoutes.walletDetail(id)) },
                onAddWallet = { navController.navigate(NavRoutes.ADD_WALLET) }
            )
        }

        composable(
            NavRoutes.WALLET_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStack ->
            val id = backStack.arguments?.getLong("id") ?: 0L
            WalletDetailScreen(
                walletId = id,
                onBack = { navController.popBackStack() },
                onReceive = { walletId -> navController.navigate(NavRoutes.receive(walletId)) }
            )
        }

        composable(NavRoutes.ADD_WALLET) {
            AddWalletScreen(onBack = { navController.popBackStack() })
        }

        composable(NavRoutes.SEND) {
            SendTokensScreen(
                onBack = { navController.popBackStack() },
                onNavigateToConfirm = { navController.navigate(NavRoutes.SEND_CONFIRM) },
                onNavigateToQrScan = { navController.navigate(NavRoutes.QR_SCAN) }
            )
        }

        composable(NavRoutes.SEND_CONFIRM) {
            SendConfirmScreen(
                onBack = { navController.popBackStack() },
                onSuccess = {
                    navController.navigate(NavRoutes.DASHBOARD) {
                        popUpTo(NavRoutes.DASHBOARD) { inclusive = true }
                    }
                }
            )
        }

        composable(
            NavRoutes.RECEIVE,
            arguments = listOf(navArgument("walletId") { type = NavType.LongType })
        ) { backStack ->
            val walletId = backStack.arguments?.getLong("walletId") ?: 0L
            ReceiveScreen(walletId = walletId, onBack = { navController.popBackStack() })
        }

        composable(NavRoutes.QR_SCAN) {
            QRScanScreen(
                onBack = { navController.popBackStack() },
                onScanned = { address ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("scanned_address", address)
                    navController.popBackStack()
                }
            )
        }

        composable(NavRoutes.BUY) {
            BuyTokensScreen(onBack = { navController.popBackStack() })
        }

        composable(NavRoutes.SELL) {
            SellTokensScreen(onBack = { navController.popBackStack() })
        }

        composable(NavRoutes.MARKETPLACE) {
            MarketplaceScreen(
                onBack = { navController.popBackStack() },
                onNavigateToBuy = { navController.navigate(NavRoutes.BUY) },
                onNavigateToSell = { navController.navigate(NavRoutes.SELL) }
            )
        }

        composable(NavRoutes.REDEEM) {
            RedeemTokensScreen(onBack = { navController.popBackStack() })
        }

        composable(NavRoutes.SUBSCRIPTIONS) {
            SubscriptionsScreen(onBack = { navController.popBackStack() })
        }

        composable(NavRoutes.TRANSACTIONS) {
            TransactionsScreen(
                onBack = { navController.popBackStack() },
                onTransactionClick = { id -> navController.navigate(NavRoutes.transactionDetail(id)) }
            )
        }

        composable(
            NavRoutes.TRANSACTION_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStack ->
            val id = backStack.arguments?.getLong("id") ?: 0L
            TransactionDetailScreen(txId = id, onBack = { navController.popBackStack() })
        }

        composable(NavRoutes.REPORTS) {
            ReportsScreen(onBack = { navController.popBackStack() })
        }

        composable(NavRoutes.CONTACTS) {
            FrequentContactsScreen(onBack = { navController.popBackStack() })
        }

        composable(NavRoutes.NOTIFICATIONS) {
            NotificationsScreen(onBack = { navController.popBackStack() })
        }

        composable(NavRoutes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLockWallet = {
                    navController.navigate(NavRoutes.AUTH) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onChangePin = { navController.navigate(NavRoutes.SETUP_PIN) }
            )
        }
    }
}
