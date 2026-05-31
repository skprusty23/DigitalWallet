package com.digitalwallet.ui.screens.reports

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digitalwallet.data.database.entity.*
import com.digitalwallet.domain.repository.*
import com.digitalwallet.ui.components.*
import com.digitalwallet.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

data class ReportsState(
    val allTransactions: List<TransactionEntity> = emptyList(),
    val selectedMonth: Month = java.time.LocalDate.now().month,
    val selectedYear: Int = java.time.LocalDate.now().year,
    val totalSent: Double = 0.0,
    val totalReceived: Double = 0.0,
    val netFlow: Double = 0.0,
    val isLoading: Boolean = false
)

@HiltViewModel
class ReportsViewModel @Inject constructor(private val transactionRepository: TransactionRepository) : ViewModel() {
    private val _state = MutableStateFlow(ReportsState())
    val state: StateFlow<ReportsState> = _state

    init {
        viewModelScope.launch {
            transactionRepository.getAll().collect { txs ->
                _state.value = _state.value.copy(allTransactions = txs)
                computeSummary()
            }
        }
    }

    fun setMonth(month: Month) {
        _state.value = _state.value.copy(selectedMonth = month)
        computeSummary()
    }

    private fun computeSummary() {
        val s = _state.value
        val monthTxs = s.allTransactions.filter {
            it.createdAt.month == s.selectedMonth && it.createdAt.year == s.selectedYear
        }
        val sent = monthTxs.filter { it.type in listOf(TransactionType.TRANSFER, TransactionType.SELL, TransactionType.REDEEM, TransactionType.SUBSCRIPTION) }.sumOf { it.amount }
        val received = monthTxs.filter { it.type in listOf(TransactionType.RECEIVE, TransactionType.BUY) }.sumOf { it.amount }
        _state.value = s.copy(totalSent = sent, totalReceived = received, netFlow = received - sent)
    }

    suspend fun exportCsv(context: android.content.Context): Uri? {
        val s = _state.value
        val monthTxs = s.allTransactions.filter {
            it.createdAt.month == s.selectedMonth && it.createdAt.year == s.selectedYear
        }
        val sb = StringBuilder("Date,Type,Counterparty,Amount,Currency,Status\n")
        monthTxs.forEach { tx ->
            sb.append("${tx.createdAt},${tx.type.name},${tx.counterpartyName},${tx.amount},${tx.currencyType.name},${tx.status.name}\n")
        }
        return withContext(Dispatchers.IO) {
            try {
                val file = File(context.cacheDir, "wallet_report_${s.selectedMonth.name}_${s.selectedYear}.csv")
                file.writeText(sb.toString())
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } catch (e: Exception) { null }
        }
    }
}

@Composable
fun ReportsScreen(onBack: () -> Unit, viewModel: ReportsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var monthDropdown by remember { mutableStateOf(false) }

    val months = Month.entries.take(12)
    val monthTxs = state.allTransactions.filter {
        it.createdAt.month == state.selectedMonth && it.createdAt.year == state.selectedYear
    }

    Scaffold(topBar = { WalletTopBar(title = "Analytics & Reports", onBack = onBack) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Box {
                    OutlinedTextField(
                        value = "${state.selectedMonth.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${state.selectedYear}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Report Period") },
                        trailingIcon = { IconButton(onClick = { monthDropdown = true }) { Icon(Icons.Default.ArrowDropDown, null) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(expanded = monthDropdown, onDismissRequest = { monthDropdown = false }) {
                        months.forEach { month ->
                            DropdownMenuItem(
                                text = { Text(month.getDisplayName(TextStyle.FULL, Locale.getDefault())) },
                                onClick = { viewModel.setMonth(month); monthDropdown = false }
                            )
                        }
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryCard("Total Sent", state.totalSent, WalletRed500, modifier = Modifier.weight(1f))
                    SummaryCard("Total Received", state.totalReceived, WalletTeal500, modifier = Modifier.weight(1f))
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (state.netFlow >= 0) WalletTeal500.copy(alpha = 0.1f) else WalletRed500.copy(alpha = 0.1f))) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Net Flow", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${if (state.netFlow >= 0) "+" else ""}${formatCurrency(state.netFlow)}",
                            fontSize = 24.sp, fontWeight = FontWeight.Bold,
                            color = if (state.netFlow >= 0) WalletTeal500 else WalletRed500
                        )
                    }
                }
            }

            item {
                Text("Transactions This Month (${monthTxs.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            // Currency breakdown
            val byCurrency = monthTxs.groupBy { it.currencyType }
            if (byCurrency.isNotEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Currency Breakdown", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            byCurrency.forEach { (currency, txs) ->
                                val total = txs.sumOf { it.amount }
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(currency.displayName, style = MaterialTheme.typography.bodyMedium)
                                    Text("${currency.symbol}${formatCurrency(total)}", fontWeight = FontWeight.Bold, color = currencyColors(currency))
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val uri = viewModel.exportCsv(context)
                                uri?.let {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/csv"
                                        putExtra(Intent.EXTRA_STREAM, it)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Export CSV"))
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export CSV")
                    }
                }
            }
        }
    }
}

fun currencyColors(currency: CurrencyType): Color = when (currency) {
    CurrencyType.USD -> UsdGreen
    CurrencyType.EUR -> EurBlue
    CurrencyType.SSD -> SsdPurple
}

@Composable
fun SummaryCard(label: String, amount: Double, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = color)
            Text(formatCurrency(amount), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
