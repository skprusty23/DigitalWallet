package com.digitalwallet.ui.screens.wallet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digitalwallet.data.database.entity.WalletEntity
import com.digitalwallet.domain.repository.WalletRepository
import com.digitalwallet.ui.components.WalletCard
import com.digitalwallet.ui.components.WalletTopBar
import com.digitalwallet.ui.components.formatCurrency
import com.digitalwallet.ui.theme.WalletNavy700
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WalletsViewModel @Inject constructor(private val walletRepository: WalletRepository) : ViewModel() {
    private val _wallets = MutableStateFlow<List<WalletEntity>>(emptyList())
    val wallets: StateFlow<List<WalletEntity>> = _wallets
    private val _totalBalance = MutableStateFlow(0.0)
    val totalBalance: StateFlow<Double> = _totalBalance

    init {
        viewModelScope.launch {
            walletRepository.getAllWallets().collect { _wallets.value = it }
        }
        viewModelScope.launch {
            walletRepository.getTotalBalance().collect { _totalBalance.value = it ?: 0.0 }
        }
    }
}

@Composable
fun WalletsScreen(
    onBack: () -> Unit,
    onWalletClick: (Long) -> Unit,
    onAddWallet: () -> Unit,
    viewModel: WalletsViewModel = hiltViewModel()
) {
    val wallets by viewModel.wallets.collectAsState()
    val totalBalance by viewModel.totalBalance.collectAsState()

    Scaffold(
        topBar = { WalletTopBar(title = "My Wallets", onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddWallet, containerColor = WalletNavy700) {
                Icon(Icons.Default.Add, contentDescription = "Add Wallet", tint = androidx.compose.ui.graphics.Color.White)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = WalletNavy700)) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Balance", color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f))
                        Text("$ ${formatCurrency(totalBalance)}", color = androidx.compose.ui.graphics.Color.White,
                            style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        Text("${wallets.size} wallets", color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            items(wallets) { wallet ->
                WalletCard(
                    wallet = wallet,
                    onClick = { onWalletClick(wallet.id) },
                    modifier = Modifier.fillMaxWidth().height(160.dp)
                )
            }
        }
    }
}
