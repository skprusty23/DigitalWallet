package com.digitalwallet.ui.screens.wallet

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digitalwallet.data.database.entity.TransactionEntity
import com.digitalwallet.data.database.entity.WalletEntity
import com.digitalwallet.domain.repository.TransactionRepository
import com.digitalwallet.domain.repository.WalletRepository
import com.digitalwallet.ui.components.*
import com.digitalwallet.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WalletDetailState(
    val wallet: WalletEntity? = null,
    val transactions: List<TransactionEntity> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class WalletDetailViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    private val transactionRepository: TransactionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val walletId = savedStateHandle.get<Long>("id") ?: 0L
    private val _state = MutableStateFlow(WalletDetailState())
    val state: StateFlow<WalletDetailState> = _state

    init {
        viewModelScope.launch {
            walletRepository.getById(walletId)?.let { wallet ->
                _state.value = _state.value.copy(wallet = wallet)
                transactionRepository.getByWallet(walletId).collect { txs ->
                    _state.value = _state.value.copy(transactions = txs, isLoading = false)
                }
            } ?: run { _state.value = _state.value.copy(isLoading = false) }
        }
    }

    fun deleteWallet(wallet: WalletEntity, onDeleted: () -> Unit) {
        viewModelScope.launch {
            walletRepository.deleteWallet(wallet)
            onDeleted()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletDetailScreen(
    walletId: Long,
    onBack: () -> Unit,
    onReceive: (Long) -> Unit,
    viewModel: WalletDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog && state.wallet != null) {
        ConfirmDialog(
            title = "Delete Wallet",
            message = "Are you sure you want to delete '${state.wallet!!.name}'? This action cannot be undone.",
            onConfirm = { viewModel.deleteWallet(state.wallet!!, onBack) },
            onDismiss = { showDeleteDialog = false }
        )
    }

    Scaffold(
        topBar = {
            WalletTopBar(
                title = state.wallet?.name ?: "Wallet Details",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.wallet == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Wallet not found")
            }
        } else {
            val wallet = state.wallet!!
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp)
                            .background(walletGradient(wallet.currencyType)).padding(24.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
                            Text(wallet.currencyType.displayName, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                            Text(
                                text = wallet.currencyType.symbol + " " + formatCurrency(wallet.balance),
                                color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StatusChip(wallet.status.name, if (wallet.status.name == "ACTIVE") WalletTeal400 else WalletRed400)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(wallet.ownerName, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                            }
                        }
                    }
                }
                item {
                    // Wallet Address
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Wallet Address", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = wallet.walletAddress,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Wallet Address", wallet.walletAddress))
                                Toast.makeText(context, "Address copied!", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                            }
                        }
                    }
                }
                item {
                    Button(
                        onClick = { onReceive(wallet.id) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WalletTeal500)
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Receive Tokens", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                item {
                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Transactions") })
                        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Info") })
                    }
                }
                if (selectedTab == 0) {
                    if (state.transactions.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("No transactions for this wallet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        items(state.transactions) { tx ->
                            TransactionItem(tx = tx)
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Divider)
                        }
                    }
                } else {
                    item {
                        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                InfoRow("Created", formatDateTime(wallet.createdAt))
                                InfoRow("Currency", wallet.currencyType.displayName)
                                InfoRow("Balance", "${wallet.currencyType.symbol} ${formatCurrency(wallet.balance)}")
                                InfoRow("Status", wallet.status.name)
                                InfoRow("Owner", wallet.ownerName)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(value, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
    }
}
