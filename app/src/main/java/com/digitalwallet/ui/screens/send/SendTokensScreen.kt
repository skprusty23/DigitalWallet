package com.digitalwallet.ui.screens.send

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digitalwallet.data.database.entity.ContactEntity
import com.digitalwallet.data.database.entity.WalletEntity
import com.digitalwallet.domain.repository.ContactRepository
import com.digitalwallet.domain.repository.WalletRepository
import com.digitalwallet.ui.components.*
import com.digitalwallet.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SendStep1State(
    val contacts: List<ContactEntity> = emptyList(),
    val favorites: List<ContactEntity> = emptyList(),
    val wallets: List<WalletEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedContact: ContactEntity? = null,
    val manualAddress: String = "",
    val selectedWallet: WalletEntity? = null,
    val amount: String = "",
    val amountError: String? = null,
    val step: Int = 1
)

object SendState {
    var recipientName: String = ""
    var recipientAddress: String = ""
    var sourceWalletId: Long = 0L
    var sourceWalletName: String = ""
    var sourceCurrencySymbol: String = ""
    var sourceCurrencyName: String = ""
    var sourceBalance: Double = 0.0
    var amount: Double = 0.0
}

@HiltViewModel
class SendTokensViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val walletRepository: WalletRepository
) : ViewModel() {
    private val _state = MutableStateFlow(SendStep1State())
    val state: StateFlow<SendStep1State> = _state

    init {
        viewModelScope.launch {
            contactRepository.getAll().collect { contacts ->
                _state.value = _state.value.copy(
                    contacts = contacts,
                    favorites = contacts.filter { it.isFavorite }
                )
            }
        }
        viewModelScope.launch {
            walletRepository.getAllWallets().collect { wallets ->
                _state.value = _state.value.copy(
                    wallets = wallets,
                    selectedWallet = wallets.firstOrNull()
                )
            }
        }
    }

    fun setSearch(q: String) {
        viewModelScope.launch {
            contactRepository.search(q).collect { results ->
                _state.value = _state.value.copy(contacts = results, searchQuery = q)
            }
        }
    }

    fun selectContact(c: ContactEntity) { _state.value = _state.value.copy(selectedContact = c, manualAddress = "") }
    fun setManualAddress(addr: String) { _state.value = _state.value.copy(manualAddress = addr, selectedContact = null) }
    fun setWallet(w: WalletEntity) { _state.value = _state.value.copy(selectedWallet = w, amountError = null) }
    fun setAmount(v: String) { _state.value = _state.value.copy(amount = v, amountError = null) }
    fun setScannedAddress(addr: String) { _state.value = _state.value.copy(manualAddress = addr, selectedContact = null) }

    fun proceedToStep2() {
        val s = _state.value
        val recipient = s.selectedContact?.walletAddress ?: s.manualAddress
        if (recipient.isBlank()) { _state.value = s.copy(amountError = "Please select or enter a recipient"); return }
        _state.value = s.copy(step = 2, amountError = null)
    }

    fun goToStep1() {
        _state.value = _state.value.copy(step = 1, amountError = null)
    }

    fun validateAndPrepareConfirm(onReady: () -> Unit) {
        val s = _state.value
        val amt = s.amount.toDoubleOrNull()
        if (amt == null || amt <= 0) { _state.value = s.copy(amountError = "Enter a valid amount"); return }
        val wallet = s.selectedWallet ?: run { _state.value = s.copy(amountError = "Select a wallet"); return }
        if (amt > wallet.balance) { _state.value = s.copy(amountError = "Insufficient balance"); return }
        val contact = s.selectedContact
        SendState.recipientName = contact?.name ?: "Manual Address"
        SendState.recipientAddress = contact?.walletAddress ?: s.manualAddress
        SendState.sourceWalletId = wallet.id
        SendState.sourceWalletName = wallet.name
        SendState.sourceCurrencySymbol = wallet.currencyType.symbol
        SendState.sourceCurrencyName = wallet.currencyType.name
        SendState.sourceBalance = wallet.balance
        SendState.amount = amt
        onReady()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendTokensScreen(
    onBack: () -> Unit,
    onNavigateToConfirm: () -> Unit,
    onNavigateToQrScan: () -> Unit,
    viewModel: SendTokensViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var walletDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { WalletTopBar(title = if (state.step == 1) "Send — Select Recipient" else "Send — Select Amount", onBack = {
            if (state.step == 2) viewModel.goToStep1()
            else onBack()
        }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.step == 1) {
                item {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.setSearch(it) },
                        label = { Text("Search contacts") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = onNavigateToQrScan) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan QR")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = state.manualAddress,
                        onValueChange = { viewModel.setManualAddress(it) },
                        label = { Text("Or enter wallet address manually") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                }
                if (state.favorites.isNotEmpty()) {
                    item { Text("Favorites", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
                    items(state.favorites) { c ->
                        ContactRow(contact = c, isSelected = state.selectedContact?.id == c.id, onClick = { viewModel.selectContact(c) })
                    }
                }
                item { Text("All Contacts", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
                items(state.contacts) { c ->
                    ContactRow(contact = c, isSelected = state.selectedContact?.id == c.id, onClick = { viewModel.selectContact(c) })
                }
                state.amountError?.let { err ->
                    item { Text(err, color = WalletRed500, style = MaterialTheme.typography.bodySmall) }
                }
                item {
                    Button(
                        onClick = { viewModel.proceedToStep2() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WalletNavy700),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Next", fontWeight = FontWeight.Bold) }
                }
            } else {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Sending To", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(state.selectedContact?.name ?: "Manual Address", fontWeight = FontWeight.Bold)
                            Text(state.selectedContact?.walletAddress ?: state.manualAddress,
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = state.selectedWallet?.name ?: "Select wallet",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("From Wallet") },
                            trailingIcon = {
                                IconButton(onClick = { walletDropdownExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            supportingText = state.selectedWallet?.let { w ->
                                {
                                    val rate = exchangeRates[w.currencyType] ?: 100.0
                                    Text("Balance: ${formatTokens(w.balance, w.currencyType)} (${w.currencyType.symbol}${formatCurrency(w.balance / rate)})")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(expanded = walletDropdownExpanded, onDismissRequest = { walletDropdownExpanded = false }) {
                            state.wallets.forEach { wallet ->
                                DropdownMenuItem(
                                    text = {
                                        val rate = exchangeRates[wallet.currencyType] ?: 100.0
                                        Text("${wallet.name} · ${formatTokens(wallet.balance, wallet.currencyType)} (${wallet.currencyType.symbol}${formatCurrency(wallet.balance / rate)})")
                                    },
                                    onClick = { viewModel.setWallet(wallet); walletDropdownExpanded = false }
                                )
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = state.amount,
                        onValueChange = { viewModel.setAmount(it) },
                        label = { Text("Amount") },
                        prefix = { Text(state.selectedWallet?.currencyType?.symbol ?: "") },
                        isError = state.amountError != null,
                        supportingText = state.amountError?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                }
                item {
                    Button(
                        onClick = { viewModel.validateAndPrepareConfirm(onNavigateToConfirm) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WalletNavy700),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Review Send", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
fun ContactRow(contact: ContactEntity, isSelected: Boolean, onClick: () -> Unit) {
    val initials = contact.name.split(" ").take(2).map { it.firstOrNull()?.uppercaseChar() ?: ' ' }.joinToString("")
    val bgColor = Color(contact.avatarColor)

    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) WalletNavy700.copy(alpha = 0.1f) else Color.Transparent)
            .border(if (isSelected) 1.dp else 0.dp, WalletNavy700.copy(alpha = if (isSelected) 0.5f else 0f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(bgColor), contentAlignment = Alignment.Center) {
            Text(initials, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(contact.name, fontWeight = FontWeight.SemiBold)
            Text(contact.email.ifBlank { contact.walletAddress.take(20) + "..." },
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (contact.isFavorite) Icon(Icons.Default.Star, contentDescription = null, tint = WalletGold500, modifier = Modifier.size(16.dp))
        if (isSelected) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = WalletTeal500)
    }
}
