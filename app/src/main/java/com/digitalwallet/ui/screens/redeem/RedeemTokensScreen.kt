package com.digitalwallet.ui.screens.redeem

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digitalwallet.data.database.entity.*
import com.digitalwallet.domain.repository.*
import com.digitalwallet.ui.components.*
import com.digitalwallet.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class RedeemState(
    val wallets: List<WalletEntity> = emptyList(),
    val selectedWallet: WalletEntity? = null,
    val tokenAmount: String = "",
    val notes: String = "",
    val redemptions: List<RedemptionEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: String? = null
)

@HiltViewModel
class RedeemViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    private val redemptionRepository: RedemptionRepository,
    private val transactionRepository: TransactionRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {
    private val _state = MutableStateFlow(RedeemState())
    val state: StateFlow<RedeemState> = _state

    init {
        viewModelScope.launch {
            walletRepository.getAllWallets().collect { wallets ->
                _state.value = _state.value.copy(wallets = wallets, selectedWallet = wallets.firstOrNull())
            }
        }
        viewModelScope.launch {
            redemptionRepository.getAll().collect { redemptions ->
                _state.value = _state.value.copy(redemptions = redemptions)
            }
        }
    }

    fun setWallet(w: WalletEntity) { _state.value = _state.value.copy(selectedWallet = w, error = null) }
    fun setAmount(v: String) { _state.value = _state.value.copy(tokenAmount = v, error = null) }
    fun setNotes(v: String) { _state.value = _state.value.copy(notes = v) }

    fun submitRedemption() {
        val s = _state.value
        val tokens = s.tokenAmount.toDoubleOrNull()
        if (tokens == null || tokens <= 0) { _state.value = s.copy(error = "Enter a valid amount"); return }
        val wallet = s.selectedWallet ?: run { _state.value = s.copy(error = "Select a wallet"); return }
        if (tokens > wallet.balance) { _state.value = s.copy(error = "Insufficient balance"); return }
        viewModelScope.launch {
            _state.value = s.copy(isLoading = true)
            walletRepository.updateBalance(wallet.id, -tokens)
            redemptionRepository.insert(RedemptionEntity(
                walletId = wallet.id, walletName = wallet.name,
                tokenAmount = tokens, currencyType = wallet.currencyType,
                status = RedemptionStatus.PENDING, notes = s.notes
            ))
            transactionRepository.insert(TransactionEntity(
                walletId = wallet.id, counterpartyName = "Redemption Center",
                amount = tokens, currencyType = wallet.currencyType, type = TransactionType.REDEEM,
                description = "Token redemption request", referenceId = UUID.randomUUID().toString()
            ))
            notificationRepository.insert(NotificationEntity(
                title = "Redemption Submitted",
                message = "Your redemption of ${formatCurrency(tokens)} ${wallet.currencyType.name} tokens is pending.",
                type = NotificationType.REDEEMED
            ))
            _state.value = _state.value.copy(isLoading = false, tokenAmount = "", notes = "",
                success = "Redemption request submitted successfully!")
        }
    }
}

fun redemptionStatusColor(status: RedemptionStatus): androidx.compose.ui.graphics.Color = when (status) {
    RedemptionStatus.PENDING -> WalletGold500
    RedemptionStatus.PROCESSING -> EurBlue
    RedemptionStatus.COMPLETED -> WalletTeal500
    RedemptionStatus.REJECTED -> WalletRed500
}

@Composable
fun RedeemTokensScreen(onBack: () -> Unit, viewModel: RedeemViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var walletDropdown by remember { mutableStateOf(false) }

    Scaffold(topBar = { WalletTopBar(title = "Redeem Tokens", onBack = onBack) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SsdPurple)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Redeem, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Redeem Tokens", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Submit a redemption request", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                    }
                }
            }
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = state.selectedWallet?.let { "${it.name} (${it.currencyType.symbol}${formatCurrency(it.balance)})" } ?: "Select Wallet",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("From Wallet") },
                        trailingIcon = {
                            IconButton(onClick = { walletDropdown = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(expanded = walletDropdown, onDismissRequest = { walletDropdown = false }) {
                        state.wallets.forEach { w ->
                            DropdownMenuItem(
                                text = { Text("${w.name} (${w.currencyType.symbol}${formatCurrency(w.balance)})") },
                                onClick = { viewModel.setWallet(w); walletDropdown = false }
                            )
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = state.tokenAmount,
                    onValueChange = { viewModel.setAmount(it) },
                    label = { Text("Token Amount") },
                    prefix = { Text(state.selectedWallet?.currencyType?.symbol ?: "") },
                    isError = state.error != null,
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = { viewModel.setNotes(it) },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(), maxLines = 3
                )
            }
            state.error?.let { item { Text(it, color = WalletRed500, style = MaterialTheme.typography.bodySmall) } }
            state.success?.let { item { Text(it, color = WalletTeal500, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold) } }
            item {
                Button(
                    onClick = { viewModel.submitRedemption() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = !state.isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = SsdPurple),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    else Text("Submit Redemption", fontWeight = FontWeight.Bold)
                }
            }
            if (state.redemptions.isNotEmpty()) {
                item { Text("Redemption History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                items(state.redemptions) { r ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(r.walletName, fontWeight = FontWeight.SemiBold)
                                Text(formatDateTime(r.requestedAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (r.notes.isNotBlank()) Text(r.notes, style = MaterialTheme.typography.bodySmall)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${r.currencyType.symbol}${formatCurrency(r.tokenAmount)}", fontWeight = FontWeight.Bold)
                                StatusChip(r.status.name, redemptionStatusColor(r.status))
                            }
                        }
                    }
                }
            }
        }
    }
}
