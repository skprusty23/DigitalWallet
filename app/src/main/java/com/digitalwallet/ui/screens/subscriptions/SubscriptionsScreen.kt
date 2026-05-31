package com.digitalwallet.ui.screens.subscriptions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Subscriptions
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
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class SubscriptionsState(
    val subscriptions: List<SubscriptionEntity> = emptyList(),
    val wallets: List<WalletEntity> = emptyList(),
    val showAddDialog: Boolean = false,
    val selectedWallet: WalletEntity? = null,
    val selectedPlan: SubscriptionPlan = SubscriptionPlan.BASIC,
    val tokenAmount: String = "100",
    val autoRenew: Boolean = true,
    val isLoading: Boolean = false
)

@HiltViewModel
class SubscriptionsViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val walletRepository: WalletRepository,
    private val transactionRepository: TransactionRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {
    private val _state = MutableStateFlow(SubscriptionsState())
    val state: StateFlow<SubscriptionsState> = _state

    init {
        viewModelScope.launch {
            subscriptionRepository.getAll().collect { subs ->
                _state.value = _state.value.copy(subscriptions = subs)
            }
        }
        viewModelScope.launch {
            walletRepository.getAllWallets().collect { wallets ->
                _state.value = _state.value.copy(wallets = wallets, selectedWallet = wallets.firstOrNull())
            }
        }
    }

    fun showDialog() { _state.value = _state.value.copy(showAddDialog = true) }
    fun hideDialog() { _state.value = _state.value.copy(showAddDialog = false) }
    fun setWallet(w: WalletEntity) { _state.value = _state.value.copy(selectedWallet = w) }
    fun setPlan(p: SubscriptionPlan) { _state.value = _state.value.copy(selectedPlan = p) }
    fun setAmount(v: String) { _state.value = _state.value.copy(tokenAmount = v) }
    fun setAutoRenew(v: Boolean) { _state.value = _state.value.copy(autoRenew = v) }

    fun createSubscription() {
        val s = _state.value
        val tokens = s.tokenAmount.toDoubleOrNull() ?: return
        val wallet = s.selectedWallet ?: return
        viewModelScope.launch {
            _state.value = s.copy(isLoading = true)
            walletRepository.updateBalance(wallet.id, -tokens)
            subscriptionRepository.insert(SubscriptionEntity(
                walletId = wallet.id, plan = s.selectedPlan,
                tokenAmount = tokens, currencyType = wallet.currencyType,
                autoRenew = s.autoRenew, startDate = LocalDate.now(),
                nextRenewalDate = LocalDate.now().plusMonths(1),
                description = "${s.selectedPlan.displayName} subscription"
            ))
            transactionRepository.insert(TransactionEntity(
                walletId = wallet.id, counterpartyName = "Subscription Service",
                amount = tokens, currencyType = wallet.currencyType, type = TransactionType.SUBSCRIPTION,
                description = "${s.selectedPlan.displayName} - monthly",
                referenceId = UUID.randomUUID().toString()
            ))
            notificationRepository.insert(NotificationEntity(
                title = "Subscription Activated",
                message = "Your ${s.selectedPlan.displayName} subscription is now active.",
                type = NotificationType.SUBSCRIPTION
            ))
            _state.value = _state.value.copy(isLoading = false, showAddDialog = false)
        }
    }

    fun toggleAutoRenew(sub: SubscriptionEntity) {
        viewModelScope.launch { subscriptionRepository.update(sub.copy(autoRenew = !sub.autoRenew)) }
    }

    fun cancelSubscription(sub: SubscriptionEntity) {
        viewModelScope.launch { subscriptionRepository.update(sub.copy(isActive = false)) }
    }
}

@Composable
fun SubscriptionsScreen(onBack: () -> Unit, viewModel: SubscriptionsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    if (state.showAddDialog) {
        AddSubscriptionDialog(
            state = state,
            onDismiss = { viewModel.hideDialog() },
            onConfirm = { viewModel.createSubscription() },
            onWalletChanged = { viewModel.setWallet(it) },
            onPlanChanged = { viewModel.setPlan(it) },
            onAmountChanged = { viewModel.setAmount(it) },
            onAutoRenewChanged = { viewModel.setAutoRenew(it) }
        )
    }

    Scaffold(
        topBar = { WalletTopBar(title = "Subscriptions", onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showDialog() }, containerColor = WalletNavy700) {
                Icon(Icons.Default.Add, contentDescription = "Add Subscription", tint = Color.White)
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val active = state.subscriptions.filter { it.isActive }
            val inactive = state.subscriptions.filter { !it.isActive }
            if (active.isNotEmpty()) {
                item { Text("Active Subscriptions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                items(active) { sub ->
                    SubscriptionCard(sub = sub,
                        onToggleAutoRenew = { viewModel.toggleAutoRenew(sub) },
                        onCancel = { viewModel.cancelSubscription(sub) })
                }
            }
            if (inactive.isNotEmpty()) {
                item { Text("Inactive Subscriptions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                items(inactive) { sub ->
                    SubscriptionCard(sub = sub, onToggleAutoRenew = {}, onCancel = {})
                }
            }
            if (state.subscriptions.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Subscriptions, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No subscriptions yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubscriptionCard(sub: SubscriptionEntity, onToggleAutoRenew: () -> Unit, onCancel: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(sub.plan.displayName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                StatusChip(if (sub.isActive) "ACTIVE" else "INACTIVE", if (sub.isActive) WalletTeal500 else WalletRed500)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("${sub.currencyType.symbol}${formatCurrency(sub.tokenAmount)} ${sub.currencyType.name}/month", style = MaterialTheme.typography.bodyMedium)
            Text("Next renewal: ${sub.nextRenewalDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Auto-renew", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(checked = sub.autoRenew, onCheckedChange = { if (sub.isActive) onToggleAutoRenew() }, enabled = sub.isActive)
                }
                if (sub.isActive) {
                    TextButton(onClick = onCancel) { Text("Cancel", color = WalletRed500) }
                }
            }
        }
    }
}

@Composable
fun AddSubscriptionDialog(
    state: SubscriptionsState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onWalletChanged: (WalletEntity) -> Unit,
    onPlanChanged: (SubscriptionPlan) -> Unit,
    onAmountChanged: (String) -> Unit,
    onAutoRenewChanged: (Boolean) -> Unit
) {
    var walletDropdown by remember { mutableStateOf(false) }
    var planDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Subscription", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box {
                    OutlinedTextField(
                        value = state.selectedPlan.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Plan") },
                        trailingIcon = { IconButton(onClick = { planDropdown = true }) { Icon(Icons.Default.ArrowDropDown, null) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(expanded = planDropdown, onDismissRequest = { planDropdown = false }) {
                        SubscriptionPlan.entries.forEach { plan ->
                            DropdownMenuItem(text = { Text(plan.displayName) }, onClick = { onPlanChanged(plan); planDropdown = false })
                        }
                    }
                }
                Box {
                    OutlinedTextField(
                        value = state.selectedWallet?.name ?: "Select Wallet",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Wallet") },
                        trailingIcon = { IconButton(onClick = { walletDropdown = true }) { Icon(Icons.Default.ArrowDropDown, null) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(expanded = walletDropdown, onDismissRequest = { walletDropdown = false }) {
                        state.wallets.forEach { w ->
                            DropdownMenuItem(text = { Text(w.name) }, onClick = { onWalletChanged(w); walletDropdown = false })
                        }
                    }
                }
                OutlinedTextField(
                    value = state.tokenAmount, onValueChange = onAmountChanged,
                    label = { Text("Monthly Token Amount") },
                    prefix = { Text(state.selectedWallet?.currencyType?.symbol ?: "") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Auto-renew monthly")
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(checked = state.autoRenew, onCheckedChange = onAutoRenewChanged)
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = !state.isLoading) {
                if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp))
                else Text("Subscribe")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
