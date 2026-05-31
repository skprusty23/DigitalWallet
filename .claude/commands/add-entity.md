# Add New Database Entity

Add a new Room entity with full Hilt wiring to the Digital Wallet app.

## What to ask me:
> /add-entity

Then tell me:
1. **Entity name** (e.g. `LoanEntity`)
2. **Table name** (e.g. `loans`)
3. **Fields** (name, type, nullable?)
4. **Queries needed** (getAll, getByWallet, getActive, search?)

## What I will create / update:
- `data/database/entity/XxxEntity.kt`
- `data/database/dao/XxxDao.kt`
- Add to `domain/repository/Repositories.kt` (interface)
- Add to `data/repository/RepositoryImpls.kt` (@Singleton impl)
- Update `di/AppModule.kt` → @Binds
- Update `di/DatabaseModule.kt` → @Provides DAO
- Update `data/database/WalletDatabase.kt` → entities list, bump version, add Migration

## Migration template:
```kotlin
val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS `loans` (...)")
    }
}
```
