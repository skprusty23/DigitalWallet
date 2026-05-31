# Add / Record a Transaction

Insert a transaction into the database through the ViewModel layer.

## What to ask me:
> /add-transaction

Then tell me:
1. **Type**: TRANSFER / RECEIVE / BUY / SELL / REDEEM / SUBSCRIPTION
2. **Amount** and **currency** (USD / EUR / SSD)
3. **Counterparty name** (who sent/received)
4. **Source wallet** (which wallet to debit/credit)
5. Any **fee**, **description**, **reference ID**?

## What I will do:
I'll write/update the ViewModel code that:
1. Validates the amount against the wallet balance
2. Calls `transactionRepository.insert(TransactionEntity(...))`
3. Calls `walletRepository.updateBalance(walletId, delta)` — delta is negative for debits
4. Shows a success/error snackbar

## Transaction types quick reference:
| Type | Balance effect | Credit? |
|---|---|---|
| RECEIVE | +amount | ✅ |
| BUY | +amount | ✅ |
| TRANSFER | -amount | ❌ |
| SELL | -amount | ❌ |
| REDEEM | -amount | ❌ |
| SUBSCRIPTION | -amount | ❌ |
