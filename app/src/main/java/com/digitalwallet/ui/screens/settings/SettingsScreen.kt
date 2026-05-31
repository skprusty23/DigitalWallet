package com.digitalwallet.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digitalwallet.security.BiometricHelper
import com.digitalwallet.security.PinManager
import com.digitalwallet.ui.components.ConfirmDialog
import com.digitalwallet.ui.components.WalletTopBar
import com.digitalwallet.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val userName: String = "Shridhar",
    val biometricEnabled: Boolean = false,
    val biometricAvailable: Boolean = false,
    val darkModeEnabled: Boolean = false,
    val showLockDialog: Boolean = false,
    val appVersion: String = "1.0.0"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val pinManager: PinManager,
    private val biometricHelper: BiometricHelper,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {
    private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state

    init {
        viewModelScope.launch {
            val name = pinManager.getUserName()
            val biometricEnabled = pinManager.isBiometricEnabled()
            val biometricAvailable = biometricHelper.isBiometricAvailable()
            _state.value = _state.value.copy(userName = name, biometricEnabled = biometricEnabled, biometricAvailable = biometricAvailable)
        }
        viewModelScope.launch {
            dataStore.data.map { it[DARK_MODE_KEY] ?: false }.collect { dark ->
                _state.value = _state.value.copy(darkModeEnabled = dark)
            }
        }
    }

    fun toggleBiometric(enabled: Boolean) {
        viewModelScope.launch { pinManager.setBiometricEnabled(enabled); _state.value = _state.value.copy(biometricEnabled = enabled) }
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch { dataStore.edit { it[DARK_MODE_KEY] = enabled }; _state.value = _state.value.copy(darkModeEnabled = enabled) }
    }

    fun showLockDialog() { _state.value = _state.value.copy(showLockDialog = true) }
    fun hideLockDialog() { _state.value = _state.value.copy(showLockDialog = false) }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLockWallet: () -> Unit,
    onChangePin: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    if (state.showLockDialog) {
        ConfirmDialog(
            title = "Lock Wallet",
            message = "Are you sure you want to lock your wallet? You'll need to enter your PIN to access it again.",
            onConfirm = { viewModel.hideLockDialog(); onLockWallet() },
            onDismiss = { viewModel.hideLockDialog() },
            confirmText = "Lock",
            dismissText = "Cancel"
        )
    }

    Scaffold(topBar = { WalletTopBar(title = "Settings", onBack = onBack) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

            // Profile section
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = WalletNavy700)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(WalletGold400), contentAlignment = Alignment.Center) {
                            Text(
                                state.userName.firstOrNull()?.uppercaseChar()?.toString() ?: "S",
                                color = WalletNavy900, fontSize = 24.sp, fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(state.userName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Wallet Owner", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                        }
                    }
                }
            }

            // Security section
            item { SectionTitle("Security") }
            item {
                SettingsCard {
                    SettingsRow(icon = Icons.Default.Lock, label = "Change PIN", onClick = onChangePin)
                    HorizontalDivider(color = Divider)
                    if (state.biometricAvailable) {
                        SettingsSwitchRow(icon = Icons.Default.Fingerprint, label = "Biometric Login",
                            checked = state.biometricEnabled, onCheckedChange = { viewModel.toggleBiometric(it) })
                    }
                }
            }

            // Preferences
            item { SectionTitle("Preferences") }
            item {
                SettingsCard {
                    SettingsSwitchRow(icon = Icons.Default.DarkMode, label = "Dark Mode",
                        checked = state.darkModeEnabled, onCheckedChange = { viewModel.toggleDarkMode(it) })
                }
            }

            // About
            item { SectionTitle("About") }
            item {
                SettingsCard {
                    SettingsInfo(icon = Icons.Default.Info, label = "App Version", value = state.appVersion)
                    HorizontalDivider(color = Divider)
                    SettingsInfo(icon = Icons.Default.AccountBalanceWallet, label = "App Name", value = "Digital Wallet")
                }
            }

            // Session
            item { SectionTitle("Session") }
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { viewModel.showLockDialog() }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LockOpen, contentDescription = null, tint = WalletRed500, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Lock Wallet", color = WalletRed500, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(content = content)
    }
}

@Composable
fun SettingsRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = WalletNavy700, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, modifier = Modifier.weight(1f), fontSize = 15.sp)
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SettingsSwitchRow(icon: ImageVector, label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = WalletNavy700, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, modifier = Modifier.weight(1f), fontSize = 15.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsInfo(icon: ImageVector, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = WalletNavy700, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, modifier = Modifier.weight(1f), fontSize = 15.sp)
        Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
    }
}
