package com.digitalwallet.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digitalwallet.data.database.entity.*
import com.digitalwallet.domain.repository.*
import com.digitalwallet.security.PinManager
import com.digitalwallet.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

data class SplashState(
    val ready: Boolean = false,
    val isPinSetup: Boolean = false
)

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val pinManager: PinManager,
    private val walletRepository: WalletRepository,
    private val contactRepository: ContactRepository,
    private val transactionRepository: TransactionRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {
    private val _state = MutableStateFlow(SplashState())
    val state: StateFlow<SplashState> = _state

    init {
        viewModelScope.launch {
            delay(1500)
            val isPinSetup = pinManager.isPinSetup()
            if (isPinSetup && !pinManager.isWalletSeeded()) {
                seedData()
            }
            _state.value = SplashState(ready = true, isPinSetup = isPinSetup)
        }
    }

    private suspend fun seedData() {
        val usdWalletId = walletRepository.insertWallet(
            WalletEntity(name = "Shridhar USD Wallet", currencyType = CurrencyType.USD, balance = 5000.0,
                walletAddress = UUID.randomUUID().toString(), ownerName = "Shridhar")
        )
        val eurWalletId = walletRepository.insertWallet(
            WalletEntity(name = "Shridhar Euro Wallet", currencyType = CurrencyType.EUR, balance = 2500.0,
                walletAddress = UUID.randomUUID().toString(), ownerName = "Shridhar")
        )
        val ssdWalletId = walletRepository.insertWallet(
            WalletEntity(name = "Shridhar SSD Token Wallet", currencyType = CurrencyType.SSD, balance = 10000.0,
                walletAddress = UUID.randomUUID().toString(), ownerName = "Shridhar")
        )

        val contacts = listOf(
            ContactEntity(name = "Bob Smith", email = "bob@example.com", walletAddress = UUID.randomUUID().toString(), preferredCurrency = CurrencyType.USD, isFavorite = true, avatarColor = 0xFF1565C0),
            ContactEntity(name = "Jack Johnson", email = "jack@example.com", walletAddress = UUID.randomUUID().toString(), preferredCurrency = CurrencyType.EUR, isFavorite = true, avatarColor = 0xFF2E7D32),
            ContactEntity(name = "Alice Williams", email = "alice@example.com", walletAddress = UUID.randomUUID().toString(), preferredCurrency = CurrencyType.USD, isFavorite = false, avatarColor = 0xFF6A1B9A),
            ContactEntity(name = "Eve Davis", email = "eve@example.com", walletAddress = UUID.randomUUID().toString(), preferredCurrency = CurrencyType.SSD, isFavorite = false, avatarColor = 0xFF00838F),
            ContactEntity(name = "Charlie Brown", email = "charlie@example.com", walletAddress = UUID.randomUUID().toString(), preferredCurrency = CurrencyType.EUR, isFavorite = false, avatarColor = 0xFFBF360C)
        )
        contacts.forEach { contactRepository.insert(it) }

        val now = LocalDateTime.now()
        transactionRepository.insert(TransactionEntity(walletId = usdWalletId, counterpartyName = "Bob Smith",
            amount = 250.0, currencyType = CurrencyType.USD, type = TransactionType.RECEIVE,
            description = "Initial transfer from Bob", referenceId = UUID.randomUUID().toString(), createdAt = now.minusDays(1)))
        transactionRepository.insert(TransactionEntity(walletId = eurWalletId, counterpartyName = "Jack Johnson",
            amount = 100.0, currencyType = CurrencyType.EUR, type = TransactionType.TRANSFER,
            description = "Payment to Jack", fee = 0.1, referenceId = UUID.randomUUID().toString(), createdAt = now.minusDays(2)))
        transactionRepository.insert(TransactionEntity(walletId = ssdWalletId, counterpartyName = "Token Exchange",
            amount = 500.0, currencyType = CurrencyType.SSD, type = TransactionType.BUY,
            description = "Purchased SSD tokens", referenceId = UUID.randomUUID().toString(), createdAt = now.minusDays(3)))

        notificationRepository.insert(NotificationEntity(title = "Welcome to Digital Wallet",
            message = "Your wallet has been set up successfully with seed data.", type = NotificationType.SYSTEM))

        pinManager.markWalletSeeded()
    }
}

@Composable
fun SplashScreen(
    onNavigateToAuth: () -> Unit,
    onNavigateToSetupPin: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val scale by animateFloatAsState(
        targetValue = if (state.ready) 1.1f else 0.8f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    LaunchedEffect(state.ready) {
        if (state.ready) {
            delay(300)
            if (state.isPinSetup) onNavigateToAuth() else onNavigateToSetupPin()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(WalletNavy900, WalletNavy700, WalletNavy600))
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .scale(scale)
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(WalletGold400, WalletGold500, WalletNavy700))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = "Wallet",
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("Digital Wallet", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Secure • Fast • Reliable", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(48.dp))
            CircularProgressIndicator(color = WalletGold400, modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
        }
    }
}
