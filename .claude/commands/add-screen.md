# Add New Screen

Add a complete new screen to the Digital Wallet app.

## What to ask me:
> /add-screen

Then tell me:
1. **Screen name** (e.g. "Loan", "Savings Goal")
2. **Module path** (e.g. `loan` → goes in `ui/screens/loan/`)
3. **What data it stores** (fields and types)
4. **Features** (list, add, detail, delete?)
5. **Route name** (e.g. `loan`, `loan_detail/{id}`)
6. **Currency type** (USD / EUR / SSD / all?)

## What I will create:
- `data/database/entity/XxxEntity.kt` — Room entity
- `data/database/dao/XxxDao.kt` — DAO
- Add interface to `domain/repository/Repositories.kt`
- Add impl to `data/repository/RepositoryImpls.kt`
- `ui/screens/xxx/XxxScreen.kt` — ViewModel + Composable
- Update `di/AppModule.kt` — @Binds
- Update `di/DatabaseModule.kt` — @Provides DAO
- Update `data/database/WalletDatabase.kt` — entity + version + Migration
- Update `ui/navigation/NavRoutes.kt` + `NavGraph.kt`
- Optionally add a Quick Action button on Dashboard

## Key rules I follow:
- `WalletTopBar` for all top bars
- `ConfirmDialog` for confirmations
- `formatCurrency()` / `formatTokens()` for amounts
- `walletGradient(currency)` for currency card backgrounds
- All data local only — no cloud
- Use `CurrencyType.USD/EUR/SSD` enum
