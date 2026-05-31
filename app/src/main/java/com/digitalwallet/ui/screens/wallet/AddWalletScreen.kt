package com.digitalwallet.ui.screens.wallet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digitalwallet.data.database.entity.CurrencyType
import com.digitalwallet.data.database.entity.WalletEntity
import com.digitalwallet.domain.repository.WalletRepository
import com.digitalwallet.ui.components.WalletTopBar
import com.digitalwallet.ui.theme.WalletNavy700
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class AddWalletState(
    val name: String = "",
    val selectedCurrency: CurrencyType = CurrencyType.USD,
    val initialBalance: String = "0",
    val nameError: String? = null,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false
)

@HiltViewModel
class AddWalletViewModel @Inject constructor(private val walletRepository: WalletRepository) : ViewModel() {
    private val _state = MutableStateFlow(AddWalletState())
    val state: StateFlow<AddWalletState> = _state

    fun setName(v: String) { _state.value = _state.value.copy(name = v, nameError = null) }
    fun setCurrency(v: CurrencyType) { _state.value = _state.value.copy(selectedCurrency = v) }
    fun setBalance(v: String) { _state.value = _state.value.copy(initialBalance = v) }

    fun save(onSaved: () -> Unit) {
        val s = _state.value
        if (s.name.isBlank()) { _state.value = s.copy(nameError = "Wallet name is required"); return }
        val balance = s.initialBalance.toDoubleOrNull() ?: 0.0
        viewModelScope.launch {
            _state.value = s.copy(isLoading = true)
            walletRepository.insertWallet(
                WalletEntity(
                    name = s.name.trim(),
                    currencyType = s.selectedCurrency,
                    balance = balance,
                    walletAddress = UUID.randomUUID().toString(),
                    ownerName = "Shridhar"
                )
            )
            onSaved()
        }
    }
}

@Composable
fun AddWalletScreen(onBack: () -> Unit, viewModel: AddWalletViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var currencyDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(topBar = { WalletTopBar(title = "Add New Wallet", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Wallet Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = state.name,
                        onValueChange = { viewModel.setName(it) },
                        label = { Text("Wallet Name") },
                        placeholder = { Text("e.g. My USD Wallet") },
                        isError = state.nameError != null,
                        supportingText = state.nameError?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Currency dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = "${state.selectedCurrency.displayName} (${state.selectedCurrency.name})",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Currency Type") },
                            trailingIcon = {
                                IconButton(onClick = { currencyDropdownExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = currencyDropdownExpanded,
                            onDismissRequest = { currencyDropdownExpanded = false }
                        ) {
                            CurrencyType.entries.forEach { currency ->
                                DropdownMenuItem(
                                    text = { Text("${currency.displayName} (${currency.name})") },
                                    onClick = { viewModel.setCurrency(currency); currencyDropdownExpanded = false }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = state.initialBalance,
                        onValueChange = { viewModel.setBalance(it) },
                        label = { Text("Initial Balance") },
                        prefix = { Text(state.selectedCurrency.symbol) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Wallet address will be auto-generated", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Button(
                onClick = { viewModel.save(onBack) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !state.isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = WalletNavy700),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = androidx.compose.ui.graphics.Color.White)
                else Text("Create Wallet", fontWeight = FontWeight.Bold)
            }
        }
    }
}
