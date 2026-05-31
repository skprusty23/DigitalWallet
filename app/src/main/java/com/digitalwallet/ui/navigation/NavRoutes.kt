package com.digitalwallet.ui.navigation

object NavRoutes {
    const val SPLASH = "splash"
    const val AUTH = "auth"
    const val SETUP_PIN = "setup_pin"
    const val DASHBOARD = "dashboard"

    // Wallets
    const val WALLETS = "wallets"
    const val WALLET_DETAIL = "wallet_detail/{id}"
    const val ADD_WALLET = "add_wallet"

    // Send / Receive
    const val SEND = "send"
    const val SEND_CONFIRM = "send_confirm"
    const val RECEIVE = "receive/{walletId}"
    const val QR_SCAN = "qr_scan"

    // Buy / Sell / Marketplace
    const val BUY = "buy"
    const val SELL = "sell"
    const val MARKETPLACE = "marketplace"

    // Redeem
    const val REDEEM = "redeem"
    const val REDEMPTION_HISTORY = "redemption_history"

    // Subscriptions
    const val SUBSCRIPTIONS = "subscriptions"

    // Transactions
    const val TRANSACTIONS = "transactions"
    const val TRANSACTION_DETAIL = "transaction_detail/{id}"

    // Reports
    const val REPORTS = "reports"

    // Contacts
    const val CONTACTS = "contacts"

    // Notifications
    const val NOTIFICATIONS = "notifications"

    // Settings
    const val SETTINGS = "settings"

    fun walletDetail(id: Long) = "wallet_detail/$id"
    fun receive(walletId: Long) = "receive/$walletId"
    fun transactionDetail(id: Long) = "transaction_detail/$id"
}
