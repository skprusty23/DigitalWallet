package com.digitalwallet.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class SetupPinState(
    val userName: String = "",
    val pin: String = "",
    val confirmPin: String = "",
    val step: SetupStep = SetupStep.NAME,
    val error: String? = null,
    val isLoading: Boolean = false
)

enum class SetupStep { NAME, ENTER_PIN, CONFIRM_PIN }

@HiltViewModel
class SetupPinViewModel @Inject constructor(
    private val pinManager: PinManager,
    private val walletRepository: WalletRepository,
    private val contactRepository: ContactRepository,
    private val transactionRepository: TransactionRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {
    private val _state = MutableStateFlow(SetupPinState())
    val state: StateFlow<SetupPinState> = _state

    fun setUserName(name: String) { _state.value = _state.value.copy(userName = name) }

    fun proceedFromName() {
        if (_state.value.userName.isBlank()) {
            _state.value = _state.value.copy(error = "Please enter your name")
            return
        }
        _state.value = _state.value.copy(step = SetupStep.ENTER_PIN, error = null)
    }

    fun addDigit(d: String) {
        val s = _state.value
        when (s.step) {
            SetupStep.ENTER_PIN -> if (s.pin.length < 6) _state.value = s.copy(pin = s.pin + d, error = null)
            SetupStep.CONFIRM_PIN -> if (s.confirmPin.length < 6) _state.value = s.copy(confirmPin = s.confirmPin + d, error = null)
            else -> {}
        }
    }

    fun removeDigit() {
        val s = _state.value
        when (s.step) {
            SetupStep.ENTER_PIN -> _state.value = s.copy(pin = s.pin.dropLast(1))
            SetupStep.CONFIRM_PIN -> _state.value = s.copy(confirmPin = s.confirmPin.dropLast(1))
            else -> {}
        }
    }

    fun proceedFromPin() {
        val s = _state.value
        if (s.pin.length < 6) { _state.value = s.copy(error = "PIN must be 6 digits"); return }
        _state.value = s.copy(step = SetupStep.CONFIRM_PIN, error = null)
    }

    fun confirmPin(onSuccess: () -> Unit) {
        val s = _state.value
        if (s.confirmPin.length < 6) { _state.value = s.copy(error = "Enter 6-digit PIN"); return }
        if (s.pin != s.confirmPin) { _state.value = s.copy(confirmPin = "", error = "PINs don't match"); return }
        viewModelScope.launch {
            _state.value = s.copy(isLoading = true)
            pinManager.setAppPin(s.pin, s.userName)
            if (!pinManager.isWalletSeeded()) { seedData(s.userName) }
            onSuccess()
        }
    }

    private suspend fun seedData(userName: String) {
        val usdId = walletRepository.insertWallet(WalletEntity(name = "$userName USD Wallet", currencyType = CurrencyType.USD, balance = 5000.0, walletAddress = UUID.randomUUID().toString(), ownerName = userName))
        val eurId = walletRepository.insertWallet(WalletEntity(name = "$userName Euro Wallet", currencyType = CurrencyType.EUR, balance = 2500.0, walletAddress = UUID.randomUUID().toString(), ownerName = userName))
        val ssdId = walletRepository.insertWallet(WalletEntity(name = "$userName SSD Token Wallet", currencyType = CurrencyType.SSD, balance = 10000.0, walletAddress = UUID.randomUUID().toString(), ownerName = userName))
        listOf(
            ContactEntity(name = "Bob Smith", email = "bob@example.com", walletAddress = UUID.randomUUID().toString(), preferredCurrency = CurrencyType.USD, isFavorite = true, avatarColor = 0xFF1565C0),
            ContactEntity(name = "Jack Johnson", email = "jack@example.com", walletAddress = UUID.randomUUID().toString(), preferredCurrency = CurrencyType.EUR, isFavorite = true, avatarColor = 0xFF2E7D32),
            ContactEntity(name = "Alice Williams", email = "alice@example.com", walletAddress = UUID.randomUUID().toString(), preferredCurrency = CurrencyType.USD, isFavorite = false, avatarColor = 0xFF6A1B9A),
            ContactEntity(name = "Eve Davis", email = "eve@example.com", walletAddress = UUID.randomUUID().toString(), preferredCurrency = CurrencyType.SSD, isFavorite = false, avatarColor = 0xFF00838F),
            ContactEntity(name = "Charlie Brown", email = "charlie@example.com", walletAddress = UUID.randomUUID().toString(), preferredCurrency = CurrencyType.EUR, isFavorite = false, avatarColor = 0xFFBF360C)
        ).forEach { contactRepository.insert(it) }
        val now = java.time.LocalDateTime.now()
        transactionRepository.insert(TransactionEntity(walletId = usdId, counterpartyName = "Bob Smith", amount = 250.0, currencyType = CurrencyType.USD, type = TransactionType.RECEIVE, description = "Initial transfer from Bob", referenceId = UUID.randomUUID().toString(), createdAt = now.minusDays(1)))
        transactionRepository.insert(TransactionEntity(walletId = eurId, counterpartyName = "Jack Johnson", amount = 100.0, currencyType = CurrencyType.EUR, type = TransactionType.TRANSFER, description = "Payment to Jack", fee = 0.1, referenceId = UUID.randomUUID().toString(), createdAt = now.minusDays(2)))
        transactionRepository.insert(TransactionEntity(walletId = ssdId, counterpartyName = "Token Exchange", amount = 500.0, currencyType = CurrencyType.SSD, type = TransactionType.BUY, description = "Purchased SSD tokens", referenceId = UUID.randomUUID().toString(), createdAt = now.minusDays(3)))
        notificationRepository.insert(NotificationEntity(title = "Welcome to Digital Wallet", message = "Your wallet has been set up with sample data.", type = NotificationType.SYSTEM))
        pinManager.markWalletSeeded()
    }
}

@Composable
fun SetupPinScreen(onPinSet: () -> Unit, viewModel: SetupPinViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(WalletNavy900, WalletNavy800))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = WalletGold400, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Setup Your Wallet", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            when (state.step) {
                SetupStep.NAME -> {
                    Text("Enter your name to get started", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(32.dp))
                    OutlinedTextField(
                        value = state.userName,
                        onValueChange = { viewModel.setUserName(it) },
                        label = { Text("Your Name", color = Color.White.copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = WalletGold400,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    state.error?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(it, color = WalletRed400, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { viewModel.proceedFromName() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WalletGold500),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Continue", color = WalletNavy900, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                }
                SetupStep.ENTER_PIN, SetupStep.CONFIRM_PIN -> {
                    val isConfirm = state.step == SetupStep.CONFIRM_PIN
                    val currentPin = if (isConfirm) state.confirmPin else state.pin
                    Text(
                        if (isConfirm) "Confirm your PIN" else "Create a 6-digit PIN",
                        color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    PinDots(pinLength = currentPin.length)
                    state.error?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(it, color = WalletRed400, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    NumPad(
                        onDigit = { viewModel.addDigit(it) },
                        onBackspace = { viewModel.removeDigit() },
                        onConfirm = if (isConfirm) { { viewModel.confirmPin(onPinSet) } } else { { viewModel.proceedFromPin() } }
                    )
                }
            }
        }
    }
}

@Composable
fun PinDots(pinLength: Int, total: Int = 6) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(total) { i ->
            Box(
                modifier = Modifier.size(16.dp).clip(CircleShape).background(
                    if (i < pinLength) WalletGold400 else Color.White.copy(alpha = 0.3f)
                )
            )
        }
    }
}

@Composable
fun NumPad(onDigit: (String) -> Unit, onBackspace: () -> Unit, onConfirm: () -> Unit) {
    val rows = listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf("","0","<"))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { key ->
                    when (key) {
                        "" -> Spacer(modifier = Modifier.size(72.dp))
                        "<" -> IconButton(
                            onClick = onBackspace,
                            modifier = Modifier.size(72.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f))
                        ) {
                            Icon(Icons.Default.Backspace, contentDescription = "Delete", tint = Color.White)
                        }
                        else -> Button(
                            onClick = { onDigit(key) },
                            modifier = Modifier.size(72.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(key, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = WalletGold500),
            shape = RoundedCornerShape(12.dp)
        ) { Text("Confirm", color = WalletNavy900, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
    }
}
