# Fix a Screen

Debug and fix an issue in an existing screen.

## What to ask me:
> /fix-screen

Then tell me:
1. **Which screen** (e.g. "Send Tokens", "Wallet Detail", "QR Scan")
2. **What's broken** (e.g. "balance not updating", "QR not generating", "crash on open")
3. Any error message or logcat output

## What I will do:
1. Read the relevant screen file(s)
2. Read ViewModel, repository, DAO
3. Check NavGraph wiring and argument passing
4. Identify root cause and fix it
5. Explain what was wrong

## Common issues I check:
- `SendState` object not populated before navigating to `SendConfirmScreen`
- `walletRepository.updateBalance()` not called after transaction insert
- CameraX `lifecycleOwner` scope issues in `QRScanScreen`
- Missing `LaunchedEffect` for post-save navigation
- `savedStateHandle["id"]` key mismatch with NavGraph argument name
- `WalletDatabase` version not bumped after entity change
