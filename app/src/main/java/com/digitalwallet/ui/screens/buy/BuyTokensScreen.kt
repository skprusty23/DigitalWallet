package com.digitalwallet.ui.screens.buy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ShoppingCart
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

data class BuyState(
    val wallets: List<WalletEntity> = emptyList(),
    val selectedWallet: WalletEntity? = null,
    val fiatAmount: String = "",
    val purchases: List<PurchaseEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: String? = null
)

@HiltViewModel
class BuyTokensViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    private val purchaseRepository: PurchaseRepository,
    private val transactionRepository: TransactionRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {
    private val _state = MutableStateFlow(BuyState())
    val state: StateFlow<BuyState> = _state

    init {
        viewModelScope.launch {
            walletRepository.getAllWallets().collect { wallets ->
                _state.value = _state.value.copy(wallets = wallets, selectedWallet = wallets.firstOrNull())
            }
        }
        viewModelScope.launch {
            purchaseRepository.getAll().collect { p ->
                _state.value = _state.value.copy(purchases = p)
            }
        }
    }

    fun setWallet(w: WalletEntity) { _state.value = _state.value.copy(selectedWallet = w, error = null) }
    fun setFiatAmount(v: String) { _state.value = _state.value.copy(fiatAmount = v, error = null, success = null) }

    fun buy() {
        val s = _state.value
        val fiat = s.fiatAmount.toDoubleOrNull()
        if (fiat == null || fiat <= 0) { _state.value = s.copy(error = "Enter a valid amount"); return }
        val wallet = s.selectedWallet ?: run { _state.value = s.copy(error = "Select a wallet"); return }
        val rate = exchangeRates[wallet.currencyType] ?: 100.0
        val tokens = fiat * rate
        viewModelScope.launch {
            _state.value = s.copy(isLoading = true)
            walletRepository.updateBalance(wallet.id, tokens)
            purchaseRepository.insert(PurchaseEntity(
                walletId = wallet.id, walletName = wallet.name,
                fiatAmount = fiat, tokenAmount = tokens, currencyType = wallet.currencyType,
                exchangeRate = rate, status = PurchaseStatus.COMPLETED
            ))
            transactionRepository.insert(TransactionEntity(
                walletId = wallet.id, counterpartyName = "Token Exchange",
                amount = tokens, currencyType = wallet.currencyType, type = TransactionType.BUY,
                description = "Purchased $tokens ${wallet.currencyType.name} tokens for $$fiat",
                referenceId = UUID.randomUUID().toString()
            ))
            notificationRepository.insert(NotificationEntity(
                title = "Tokens Purchased",
                message = "You bought ${formatCurrency(tokens)} ${wallet.currencyType.name} tokens for $${formatCurrency(fiat)}",
                type = NotificationType.PURCHASED
            ))
            _state.value = _state.value.copy(isLoading = false, fiatAmount = "", success = "Successfully purchased ${formatCurrency(tokens)} tokens!")
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
fun BuyTokensScreen(onBack: () -> Unit, viewModel: BuyTokensViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var walletDropdown by remember { mutableStateOf(false) }
    val wallet = state.selectedWallet
    val fiat = state.fiatAmount.toDoubleOrNull() ?: 0.0
    val rate = wallet?.let { exchangeRates[it.currencyType] } ?: 100.0
    val tokensToReceive = fiat * rate

    Scaffold(topBar = { WalletTopBar(title = "Buy Tokens", onBack = onBack) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = WalletNavy700)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = WalletGold400, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Buy Digital Tokens", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Instant purchase with exchange rate", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                    }
                }
            }
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = wallet?.name ?: "Select Wallet",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Destination Wallet") },
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
                                text = { Text("${w.name} (${w.currencyType.name})") },
                                onClick = { viewModel.setWallet(w); walletDropdown = false }
                            )
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = state.fiatAmount,
                    onValueChange = { viewModel.setFiatAmount(it) },
                    label = { Text("Fiat Amount to Spend") },
                    prefix = { Text(wallet?.currencyType?.symbol ?: "$") },
                    suffix = { Text(wallet?.currencyType?.tokenSymbol ?: "USDT") },
                    isError = state.error != null,
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
            }
            if (wallet != null && fiat > 0) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Exchange Summary", fontWeight = FontWeight.Bold)
                            DetailRow("Rate", "1 ${wallet.currencyType.symbol} = ${rate} ${wallet.currencyType.tokenSymbol}")
                            DetailRow("You Pay", "${wallet.currencyType.symbol}${formatCurrency(fiat)}")
                            DetailRow("You Receive", "${formatTokens(tokensToReceive, wallet.currencyType)} (${wallet.currencyType.symbol}${formatCurrency(fiat)})", isBold = true)
                        }
                    }
                }
            }
            state.error?.let { item { Text(it, color = WalletRed500, style = MaterialTheme.typography.bodySmall) } }
            state.success?.let { item { Text(it, color = WalletTeal500, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold) } }
            item {
                Button(
                    onClick = { viewModel.buy() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = !state.isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = WalletNavy700),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    else Text("Buy Tokens", fontWeight = FontWeight.Bold)
                }
            }
            if (state.purchases.isNotEmpty()) {
                item { Text("Purchase History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                items(state.purchases) { p ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(p.walletName, fontWeight = FontWeight.SemiBold)
                                Text(formatDateTime(p.purchasedAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("+${formatTokens(p.tokenAmount, p.currencyType)}", color = WalletTeal500, fontWeight = FontWeight.Bold)
                                Text("Paid: ${p.currencyType.symbol}${formatCurrency(p.fiatAmount)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
