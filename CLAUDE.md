# Digital Wallet App

## Project Location
`E:\poc\digitalWallet`

## What This App Does
Enterprise-grade digital wallet for managing and transferring multi-currency tokens (USD / EUR / SSD). Features QR code generation & scanning, buy/sell/redeem flows, subscriptions, marketplace, analytics, and notifications. All data is stored **locally only** — no cloud sync ever.

## Tech Stack
| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| DI | Hilt |
| Database | Room + SQLCipher (AES-256 encrypted) |
| Prefs | DataStore Preferences |
| Auth | PIN (SHA-256 + salt) + Biometric |
| QR Generate | ZXing (`MultiFormatWriter`) |
| QR Scan | CameraX + ML Kit BarcodeScanner |
| Charts | Vico (`compose-m3`) |
| Nav | Navigation Compose |
| Min SDK | 26 (Android 8) |

## Package
`com.digitalwallet`

## Architecture (MVVM + Clean)
```
app/src/main/java/com/digitalwallet/
├── data/
│   ├── database/
│   │   ├── entity/          ← 7 Room @Entity classes
│   │   ├── dao/             ← 7 @Dao interfaces
│   │   ├── converter/       ← Converters.kt (LocalDateTime)
│   │   └── WalletDatabase.kt
│   └── repository/
│       └── RepositoryImpls.kt  ← All 7 @Singleton impls
├── domain/
│   └── repository/
│       └── Repositories.kt  ← 7 interface definitions
├── security/
│   ├── SecurityManager.kt   ← AES/GCM DB key + encrypt/decrypt
│   ├── PinManager.kt        ← PIN hash, biometric, seed flag, user name
│   └── BiometricHelper.kt
├── di/
│   ├── DatabaseModule.kt    ← Hilt provides DB + 7 DAOs
│   └── AppModule.kt         ← Hilt binds 7 repositories
└── ui/
    ├── components/
    │   └── WalletComponents.kt  ← WalletTopBar, WalletCard, TransactionItem,
    │                                QuickActionButton, CurrencyBadge, ConfirmDialog,
    │                                SectionHeader, StatusChip,
    │                                formatCurrency(), formatTokens(), formatDateTime()
    ├── navigation/
    │   ├── NavRoutes.kt
    │   └── NavGraph.kt
    ├── screens/
    │   ├── auth/            ← AuthScreen, SetupPinScreen (seeds data on first setup)
    │   ├── splash/          ← SplashScreen (fallback seed, routes to auth/setup)
    │   ├── dashboard/       ← DashboardScreen (portfolio card, wallet carousel, quick actions, market overview, bottom nav)
    │   ├── wallet/          ← WalletsScreen, WalletDetailScreen, AddWalletScreen
    │   ├── send/            ← SendTokensScreen (2-step), SendConfirmScreen
    │   ├── receive/         ← ReceiveScreen (ZXing QR), QRScanScreen (CameraX + ML Kit)
    │   ├── buy/             ← BuyTokensScreen
    │   ├── sell/            ← SellTokensScreen
    │   ├── marketplace/     ← MarketplaceScreen
    │   ├── redeem/          ← RedeemTokensScreen
    │   ├── subscriptions/   ← SubscriptionsScreen
    │   ├── transactions/    ← TransactionsScreen, TransactionDetailScreen
    │   ├── reports/         ← ReportsScreen (CSV export, monthly summary)
    │   ├── notifications/   ← NotificationsScreen
    │   ├── contacts/        ← FrequentContactsScreen
    │   └── settings/        ← SettingsScreen (lock, change PIN, biometric)
    └── theme/
        ├── Color.kt         ← WalletNavy900/800/700/600/400, WalletGold500/400/300,
        │                       WalletTeal500/400, WalletRed500, UsdGreen, EurBlue, SsdPurple
        ├── Type.kt          ← WalletTypography (FontFamily.Default)
        └── Theme.kt         ← DigitalWalletTheme (light + dark)
```

## Currencies
```kotlin
enum class CurrencyType(val displayName: String, val symbol: String) {
    USD("USD Token", "$"),
    EUR("Euro Token", "€"),
    SSD("SSD Token", "S")
}
```
Wallet card gradients: USD = green, EUR = blue, SSD = purple.

## Entities
| Entity | Table | Key Fields |
|---|---|---|
| `WalletEntity` | `wallets` | `currencyType`, `balance`, `walletAddress`, `status` |
| `TransactionEntity` | `transactions` | `walletId`, `type`, `amount`, `currencyType`, `counterpartyName` |
| `ContactEntity` | `contacts` | `walletAddress`, `preferredCurrency`, `isFavorite`, `avatarColor` |
| `SubscriptionEntity` | `subscriptions` | `plan`, `currencyType`, `monthlyFee`, `nextBillingDate` |
| `RedemptionEntity` | `redemptions` | `walletId`, `amount`, `currencyType`, `status` |
| `PurchaseEntity` | `purchases` | `walletId`, `amount`, `currencyType`, `status` |
| `NotificationEntity` | `notifications` | `type`, `title`, `message`, `isRead` |

## Key Patterns

### Adding a New Screen
Every screen file contains **both** ViewModel and Composable in the same file.

```kotlin
// 1. State data class
data class MyScreenState(val isLoading: Boolean = true, val error: String? = null)

// 2. HiltViewModel
@HiltViewModel
class MyScreenViewModel @Inject constructor(
    private val walletRepository: WalletRepository
) : ViewModel() {
    private val _state = MutableStateFlow(MyScreenState())
    val state: StateFlow<MyScreenState> = _state
    init { /* load data */ }
}

// 3. Composable screen
@Composable
fun MyScreen(onBack: () -> Unit, viewModel: MyScreenViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    Scaffold(topBar = { WalletTopBar(title = "My Screen", onBack = onBack) }) { padding ->
        // content
    }
}
```

### Adding a New Entity
1. Create `data/database/entity/MyEntity.kt`
2. Create `data/database/dao/MyDao.kt`
3. Add interface to `domain/repository/Repositories.kt`
4. Add implementation to `data/repository/RepositoryImpls.kt`
5. Add `@Binds` in `AppModule.kt`
6. Add `@Provides` DAO in `DatabaseModule.kt`
7. Add entity to `@Database(entities = [...])` in `WalletDatabase.kt`
8. Bump `version` and add `Migration`

### Adding a Route
```kotlin
// NavRoutes.kt
const val MY_SCREEN = "my_screen"

// NavGraph.kt
composable(NavRoutes.MY_SCREEN) {
    MyScreen(onBack = { navController.popBackStack() })
}
```

### QR Code Generation (ZXing)
```kotlin
val bitmap = remember(walletAddress) {
    MultiFormatWriter().encode(walletAddress, BarcodeFormat.QR_CODE, 512, 512).let { matrix ->
        Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565).apply {
            for (x in 0 until 512) for (y in 0 until 512)
                setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
        }
    }
}
Image(bitmap = bitmap.asImageBitmap(), contentDescription = "QR")
```

### Lock / Logout Pattern
```kotlin
navController.navigate(NavRoutes.AUTH) {
    popUpTo(0) { inclusive = true }   // clears full back stack, PIN kept
}
```

### Passing Data Between Send Screens
`SendState` is a Kotlin `object` (in-memory singleton) used to pass send parameters from `SendTokensScreen` → `SendConfirmScreen`. Not persisted.

## Security Rules (DO NOT CHANGE)
- DB key = 32-byte random bytes, AES/GCM encrypted, stored in SharedPreferences via AndroidKeyStore.
- IV is always prepended to ciphertext (first 12 bytes).
- **No cloud sync. No Firebase. No Drive.** Local only.

## Seed Data
Triggered in `SetupPinScreen.confirmPin()` on first-ever PIN setup. Also guarded in `SplashScreen` as fallback. Seeded: 3 wallets (USD/EUR/SSD), 5 contacts, 3 transactions, 1 welcome notification. Seeded flag stored in DataStore via `PinManager.markWalletSeeded()`.

## Useful Formatting Functions (in WalletComponents.kt)
```kotlin
formatCurrency(1234.5)          // → "1,234.50"
formatTokens(100.0, CurrencyType.USD)  // → "100.00 USD"
formatDateTime(localDateTime)   // → "Jan 15, 2025 14:30"
formatDate(localDateTime)       // → "Jan 15, 2025"
walletGradient(CurrencyType.USD) // → Brush for WalletCard background
currencyColor(CurrencyType.EUR) // → EurBlue Color
```

## Git Workflow
Branch: `master` | Local only (no remote configured yet)
```
git add <files>
git commit -m "feat: describe what changed"
# To add remote: git remote add origin <url> && git push -u origin master
```

## Build
Open `E:\poc\digitalWallet` in **Android Studio**. Gradle sync downloads all deps.
Run on device/emulator (minSdk 26). Camera permission required for QR scan.
