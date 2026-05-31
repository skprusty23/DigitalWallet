package com.digitalwallet.ui.screens.receive

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digitalwallet.data.database.entity.WalletEntity
import com.digitalwallet.domain.repository.WalletRepository
import com.digitalwallet.ui.components.WalletTopBar
import com.digitalwallet.ui.components.walletGradient
import com.digitalwallet.ui.theme.WalletTeal500
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReceiveViewModel @Inject constructor(private val walletRepository: WalletRepository) : ViewModel() {
    private val _wallet = MutableStateFlow<WalletEntity?>(null)
    val wallet: StateFlow<WalletEntity?> = _wallet

    fun loadWallet(walletId: Long) {
        viewModelScope.launch {
            if (walletId > 0) {
                _wallet.value = walletRepository.getById(walletId)
            } else {
                walletRepository.getAllWallets().collect { wallets ->
                    if (_wallet.value == null) _wallet.value = wallets.firstOrNull()
                }
            }
        }
    }
}

fun generateQrBitmap(content: String, size: Int = 512): Bitmap? = try {
    val writer = MultiFormatWriter()
    val matrix: BitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
    for (x in 0 until size) for (y in 0 until size) {
        bmp.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
    }
    bmp
} catch (e: Exception) { null }

@Composable
fun ReceiveScreen(
    walletId: Long,
    onBack: () -> Unit,
    viewModel: ReceiveViewModel = hiltViewModel()
) {
    val wallet by viewModel.wallet.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(walletId) { viewModel.loadWallet(walletId) }

    val qrBitmap = remember(wallet?.walletAddress) {
        wallet?.walletAddress?.let { generateQrBitmap(it) }
    }

    Scaffold(topBar = { WalletTopBar(title = "Receive Tokens", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            wallet?.let { w ->
                Text(w.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${w.currencyType.displayName} Wallet", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(24.dp))

                // QR Code Card
                Card(
                    modifier = Modifier.size(280.dp),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                        if (qrBitmap != null) {
                            Image(bitmap = qrBitmap.asImageBitmap(), contentDescription = "QR Code",
                                modifier = Modifier.fillMaxSize())
                        } else {
                            CircularProgressIndicator()
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Address display
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Wallet Address", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(w.walletAddress, style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Wallet Address", w.walletAddress))
                            Toast.makeText(context, "Address copied!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy", color = Color.White)
                    }
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Send tokens to my ${w.currencyType.displayName} wallet:\n${w.walletAddress}")
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Wallet Address"))
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = WalletTeal500)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Share your wallet address or QR code to receive tokens. Only share with trusted parties.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}
