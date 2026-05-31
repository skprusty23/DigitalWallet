package com.digitalwallet.ui.screens.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digitalwallet.data.database.entity.TransactionEntity
import com.digitalwallet.data.database.entity.TransactionType
import com.digitalwallet.domain.repository.TransactionRepository
import com.digitalwallet.ui.components.*
import com.digitalwallet.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val txId = savedStateHandle.get<Long>("id") ?: 0L
    private val _tx = MutableStateFlow<TransactionEntity?>(null)
    val tx: StateFlow<TransactionEntity?> = _tx

    init {
        viewModelScope.launch { _tx.value = transactionRepository.getById(txId) }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(value, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun TransactionDetailScreen(txId: Long, onBack: () -> Unit, viewModel: TransactionDetailViewModel = hiltViewModel()) {
    val tx by viewModel.tx.collectAsState()

    val isCredit = tx?.type == TransactionType.RECEIVE || tx?.type == TransactionType.BUY
    val amountColor = if (isCredit) WalletTeal500 else WalletRed500
    val amountPrefix = if (isCredit) "+" else "-"

    val icon: ImageVector = when (tx?.type) {
        TransactionType.RECEIVE -> Icons.Default.ArrowDownward
        TransactionType.TRANSFER -> Icons.Default.ArrowUpward
        TransactionType.BUY -> Icons.Default.ShoppingCart
        TransactionType.SELL -> Icons.Default.TrendingUp
        TransactionType.REDEEM -> Icons.Default.Redeem
        TransactionType.SUBSCRIPTION -> Icons.Default.Subscriptions
        null -> Icons.Default.Receipt
    }

    Scaffold(topBar = { WalletTopBar(title = "Transaction Details", onBack = onBack) }) { padding ->
        tx?.let { t ->
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Amount hero
                Card(
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = amountColor.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(amountColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center) {
                            Icon(icon, contentDescription = null, tint = amountColor, modifier = Modifier.size(30.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("$amountPrefix${t.currencyType.symbol}${formatCurrency(t.amount)}", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = amountColor)
                        Text(t.type.name, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    }
                }

                // Details
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Transaction Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        HorizontalDivider(color = Divider)
                        InfoRow("Counterparty", t.counterpartyName)
                        if (t.counterpartyAddress.isNotBlank()) InfoRow("Address", t.counterpartyAddress.take(24) + "...")
                        InfoRow("Currency", t.currencyType.displayName)
                        InfoRow("Amount", "${t.currencyType.symbol}${formatCurrency(t.amount)}")
                        if (t.fee > 0) InfoRow("Network Fee", "${t.currencyType.symbol}${formatCurrency(t.fee)}")
                        InfoRow("Status", t.status.name)
                        InfoRow("Date & Time", formatDateTime(t.createdAt))
                        if (t.referenceId.isNotBlank()) InfoRow("Reference ID", t.referenceId)
                        if (t.description.isNotBlank()) InfoRow("Description", t.description)
                    }
                }

                // Status badge
                StatusChip(t.status.name, when (t.status.name) {
                    "COMPLETED" -> WalletTeal500
                    "PENDING" -> WalletGold500
                    "FAILED" -> WalletRed500
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                })
            }
        } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}
