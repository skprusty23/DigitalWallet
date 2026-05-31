package com.digitalwallet.ui.screens.send

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
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

data class ConfirmSendState(
    val isLoading: Boolean = false,
    val isSent: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SendConfirmViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    private val transactionRepository: TransactionRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ConfirmSendState())
    val state: StateFlow<ConfirmSendState> = _state

    fun confirmSend(onSuccess: () -> Unit) {
        val s = SendState
        if (s.sourceWalletId == 0L) {
            _state.value = _state.value.copy(error = "Invalid send state")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val fee = s.amount * 0.001
            walletRepository.updateBalance(s.sourceWalletId, -(s.amount + fee))
            transactionRepository.insert(
                TransactionEntity(
                    walletId = s.sourceWalletId,
                    counterpartyName = s.recipientName,
                    counterpartyAddress = s.recipientAddress,
                    amount = s.amount,
                    currencyType = CurrencyType.valueOf(s.sourceCurrencyName),
                    type = TransactionType.TRANSFER,
                    status = TransactionStatus.COMPLETED,
                    description = "Token transfer to ${s.recipientName}",
                    fee = fee,
                    referenceId = UUID.randomUUID().toString()
                )
            )
            notificationRepository.insert(
                NotificationEntity(
                    title = "Transfer Sent",
                    message = "You sent ${s.sourceCurrencySymbol}${formatCurrency(s.amount)} to ${s.recipientName}",
                    type = NotificationType.SENT
                )
            )
            _state.value = _state.value.copy(isLoading = false, isSent = true)
            onSuccess()
        }
    }
}

@Composable
fun SendConfirmScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: SendConfirmViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val s = SendState
    val fee = s.amount * 0.001
    val total = s.amount + fee

    Scaffold(topBar = { WalletTopBar(title = "Confirm Transfer", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Amount display
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = WalletNavy700)
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Sending", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                    Text(
                        "${s.sourceCurrencySymbol} ${formatCurrency(s.amount)} ${s.sourceCurrencyName}",
                        color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold
                    )
                }
            }

            // Details card
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Transfer Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    HorizontalDivider(color = Divider)
                    DetailRow("From", s.sourceWalletName)
                    DetailRow("To", s.recipientName)
                    DetailRow("Address", s.recipientAddress.take(24) + "...")
                    DetailRow("Balance Before", "${s.sourceCurrencySymbol} ${formatCurrency(s.sourceBalance)}")
                    HorizontalDivider(color = Divider)
                    DetailRow("Amount", "${s.sourceCurrencySymbol} ${formatCurrency(s.amount)}")
                    DetailRow("Network Fee (0.1%)", "${s.sourceCurrencySymbol} ${formatCurrency(fee)}")
                    HorizontalDivider(color = Divider)
                    DetailRow("Total Deducted", "${s.sourceCurrencySymbol} ${formatCurrency(total)}", isBold = true)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            state.error?.let {
                Text(it, color = WalletRed500, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = { viewModel.confirmSend(onSuccess) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !state.isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = WalletTeal500),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirm & Send", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Cancel") }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, isBold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(value, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal, style = MaterialTheme.typography.bodyMedium)
    }
}
