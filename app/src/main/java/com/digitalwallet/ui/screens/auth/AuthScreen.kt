package com.digitalwallet.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digitalwallet.security.BiometricHelper
import com.digitalwallet.security.PinManager
import com.digitalwallet.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthState(
    val pin: String = "",
    val error: String? = null,
    val userName: String = "Shridhar",
    val biometricEnabled: Boolean = false,
    val isLoading: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val pinManager: PinManager,
    private val biometricHelper: BiometricHelper
) : ViewModel() {
    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state

    init {
        viewModelScope.launch {
            val userName = pinManager.getUserName()
            val biometricEnabled = pinManager.isBiometricEnabled() && biometricHelper.isBiometricAvailable()
            _state.value = _state.value.copy(userName = userName, biometricEnabled = biometricEnabled)
        }
    }

    fun addDigit(d: String) {
        if (_state.value.pin.length < 6) _state.value = _state.value.copy(pin = _state.value.pin + d, error = null)
    }

    fun removeDigit() { _state.value = _state.value.copy(pin = _state.value.pin.dropLast(1)) }

    fun verify(onSuccess: () -> Unit) {
        val pin = _state.value.pin
        if (pin.length < 6) { _state.value = _state.value.copy(error = "Enter all 6 digits"); return }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            if (pinManager.verifyAppPin(pin)) {
                onSuccess()
            } else {
                _state.value = _state.value.copy(pin = "", error = "Incorrect PIN. Try again.", isLoading = false)
            }
        }
    }

    fun biometricAuthenticate(activity: FragmentActivity, onSuccess: () -> Unit) {
        biometricHelper.authenticate(
            activity = activity,
            onSuccess = onSuccess,
            onError = { err -> _state.value = _state.value.copy(error = err) }
        )
    }
}

@Composable
fun AuthScreen(onAuthSuccess: () -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (state.biometricEnabled) {
            viewModel.biometricAuthenticate(context as FragmentActivity, onAuthSuccess)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(WalletNavy900, WalletNavy800))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = WalletGold400, modifier = Modifier.size(72.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Digital Wallet", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Good day, ${state.userName}", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Enter your PIN to unlock", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
            Spacer(modifier = Modifier.height(32.dp))
            PinDots(pinLength = state.pin.length)
            state.error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = WalletRed400, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
            Spacer(modifier = Modifier.height(32.dp))
            NumPad(
                onDigit = { viewModel.addDigit(it) },
                onBackspace = { viewModel.removeDigit() },
                onConfirm = { viewModel.verify(onAuthSuccess) }
            )
            if (state.biometricEnabled) {
                Spacer(modifier = Modifier.height(24.dp))
                TextButton(onClick = {
                    viewModel.biometricAuthenticate(context as FragmentActivity, onAuthSuccess)
                }) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = WalletGold400, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Use Biometric", color = WalletGold400)
                }
            }
        }
    }
}
