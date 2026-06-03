package com.digitalwallet.ui.screens.sell

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.TrendingUp
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

data class SellState(
    val wallets: List<WalletEntity> = emptyList(),
    val selectedWallet: WalletEntity? = null,
    val tokenAmount: String = "",
    val transactions: List<TransactionEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: String? = null
)

@HiltViewModel
class SellTokensViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    private val transactionRepository: TransactionRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {
    private val _state = MutableStateFlow(SellState())
    val state: StateFlow<SellState> = _state

    init {
        viewModelScope.launch {
            walletRepository.getAllWallets().collect { wallets ->
                _state.value = _state.value.copy(wallets = wallets, selectedWallet = wallets.firstOrNull())
            }
        }
        viewModelScope.launch {
            transactionRepository.getByType(TransactionType.SELL).collect { txs ->
                _state.value = _state.value.copy(transactions = txs)
            }
        }
    }

    fun setWallet(w: WalletEntity) { _state.value = _state.value.copy(selectedWallet = w, error = null) }
    fun setAmount(v: String) { _state.value = _state.value.copy(tokenAmount = v, error = null, success = null) }

    fun sell() {
        val s = _state.value
        val tokens = s.tokenAmount.toDoubleOrNull()
        if (tokens == null || tokens <= 0) { _state.value = s.copy(error = "Enter a valid amount"); return }
        val wallet = s.selectedWallet ?: run { _state.value = s.copy(error = "Select a wallet"); return }
        if (tokens > wallet.balance) { _state.value = s.copy(error = "Insufficient balance. Available: ${formatTokens(wallet.balance, wallet.currencyType)}"); return }
        val rate = exchangeRates[wallet.currencyType] ?: 100.0
        val fiatReceived = tokens / rate
        viewModelScope.launch {
            _state.value = s.copy(isLoading = true)
            walletRepository.updateBalance(wallet.id, -tokens)
            transactionRepository.insert(TransactionEntity(
                walletId = wallet.id, counterpartyName = "Token Exchange",
                amount = tokens, currencyType = wallet.currencyType, type = TransactionType.SELL,
                description = "Sold $tokens ${wallet.currencyType.name} tokens for $$fiatReceived",
                referenceId = UUID.randomUUID().toString()
            ))
            notificationRepository.insert(NotificationEntity(
                title = "Tokens Sold",
                message = "You sold ${formatCurrency(tokens)} ${wallet.currencyType.name} for $${formatCurrency(fiatReceived)}",
                type = NotificationType.SENT
            ))
            _state.value = _state.value.copy(isLoading = false, tokenAmount = "",
                success = "Sold ${formatCurrency(tokens)} tokens for $${formatCurrency(fiatReceived)} USD!")
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, isBold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(value, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun SellTokensScreen(onBack: () -> Unit, viewModel: SellTokensViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var walletDropdown by remember { mutableStateOf(false) }
    val wallet = state.selectedWallet
    val tokens = state.tokenAmount.toDoubleOrNull() ?: 0.0
    val rate = wallet?.let { exchangeRates[it.currencyType] } ?: 100.0
    val fiatEquivalent = tokens / rate

    Scaffold(topBar = { WalletTopBar(title = "Sell Tokens", onBack = onBack) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = WalletGold500)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = WalletNavy900, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Sell Your Tokens", color = WalletNavy900, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Convert tokens back to fiat", color = WalletNavy900.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                    }
                }
            }
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = wallet?.let { "${it.name} (${formatTokens(it.balance, it.currencyType)})" } ?: "Select Wallet",
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
                                text = {
                                    val r = exchangeRates[w.currencyType] ?: 100.0
                                    Text("${w.name} · ${formatTokens(w.balance, w.currencyType)} (${w.currencyType.symbol}${formatCurrency(w.balance / r)})")
                                },
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
                    label = { Text("Token Amount to Sell") },
                    suffix = { Text(wallet?.currencyType?.tokenSymbol ?: "") },
                    isError = state.error != null,
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
            }
            if (wallet != null && tokens > 0) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Sale Summary", fontWeight = FontWeight.Bold)
                            DetailRow("Rate", "${formatCurrency(rate)} ${wallet.currencyType.tokenSymbol} = 1 ${wallet.currencyType.symbol}")
                            DetailRow("Tokens to Sell", formatTokens(tokens, wallet.currencyType))
                            DetailRow("You Receive", "${formatTokens(tokens, wallet.currencyType)} (${wallet.currencyType.symbol}${formatCurrency(fiatEquivalent)})", isBold = true)
                        }
                    }
                }
            }
            state.error?.let { item { Text(it, color = WalletRed500, style = MaterialTheme.typography.bodySmall) } }
            state.success?.let { item { Text(it, color = WalletTeal500, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold) } }
            item {
                Button(
                    onClick = { viewModel.sell() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = !state.isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = WalletGold500),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = WalletNavy900)
                    else Text("Sell Tokens", fontWeight = FontWeight.Bold, color = WalletNavy900)
                }
            }
            if (state.transactions.isNotEmpty()) {
                item { Text("Sell History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                items(state.transactions.take(10)) { tx ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(tx.counterpartyName, fontWeight = FontWeight.SemiBold)
                                Text(formatDateTime(tx.createdAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("-${formatTokens(tx.amount, tx.currencyType)}", color = WalletRed500, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
