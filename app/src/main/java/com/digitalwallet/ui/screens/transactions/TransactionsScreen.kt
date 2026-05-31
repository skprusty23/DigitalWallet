package com.digitalwallet.ui.screens.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digitalwallet.data.database.entity.CurrencyType
import com.digitalwallet.data.database.entity.TransactionEntity
import com.digitalwallet.data.database.entity.TransactionType
import com.digitalwallet.domain.repository.TransactionRepository
import com.digitalwallet.ui.components.*
import com.digitalwallet.ui.theme.Divider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class TransactionsState(
    val allTransactions: List<TransactionEntity> = emptyList(),
    val filtered: List<TransactionEntity> = emptyList(),
    val typeFilter: TransactionType? = null,
    val currencyFilter: CurrencyType? = null,
    val searchQuery: String = ""
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(private val transactionRepository: TransactionRepository) : ViewModel() {
    private val _state = MutableStateFlow(TransactionsState())
    val state: StateFlow<TransactionsState> = _state

    init {
        viewModelScope.launch {
            transactionRepository.getAll().collect { txs ->
                _state.value = _state.value.copy(allTransactions = txs)
                applyFilters()
            }
        }
    }

    fun setSearch(q: String) { _state.value = _state.value.copy(searchQuery = q); applyFilters() }
    fun setTypeFilter(type: TransactionType?) { _state.value = _state.value.copy(typeFilter = type); applyFilters() }
    fun setCurrencyFilter(c: CurrencyType?) { _state.value = _state.value.copy(currencyFilter = c); applyFilters() }

    private fun applyFilters() {
        val s = _state.value
        val result = s.allTransactions.filter { tx ->
            (s.typeFilter == null || tx.type == s.typeFilter) &&
            (s.currencyFilter == null || tx.currencyType == s.currencyFilter) &&
            (s.searchQuery.isBlank() || tx.counterpartyName.contains(s.searchQuery, ignoreCase = true) ||
                tx.description.contains(s.searchQuery, ignoreCase = true))
        }
        _state.value = s.copy(filtered = result)
    }
}

fun groupTransactionsByDate(txs: List<TransactionEntity>): Map<String, List<TransactionEntity>> {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)
    val weekAgo = today.minusDays(7)
    return txs.groupBy { tx ->
        val date = tx.createdAt.toLocalDate()
        when {
            date == today -> "Today"
            date == yesterday -> "Yesterday"
            date.isAfter(weekAgo) -> "This Week"
            else -> "Earlier"
        }
    }
}

@Composable
fun TransactionsScreen(
    onBack: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val grouped = remember(state.filtered) { groupTransactionsByDate(state.filtered) }
    val groupOrder = listOf("Today", "Yesterday", "This Week", "Earlier")

    Scaffold(topBar = { WalletTopBar(title = "Transaction History", onBack = onBack) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.setSearch(it) },
                    label = { Text("Search transactions") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().padding(16.dp), singleLine = true
                )
            }
            item {
                LazyRow(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(selected = state.typeFilter == null, onClick = { viewModel.setTypeFilter(null) }, label = { Text("All") })
                    }
                    items(TransactionType.entries) { type ->
                        FilterChip(selected = state.typeFilter == type, onClick = { viewModel.setTypeFilter(if (state.typeFilter == type) null else type) },
                            label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) })
                    }
                }
            }
            item {
                LazyRow(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(selected = state.currencyFilter == null, onClick = { viewModel.setCurrencyFilter(null) }, label = { Text("All Currencies") })
                    }
                    items(CurrencyType.entries) { currency ->
                        FilterChip(selected = state.currencyFilter == currency, onClick = { viewModel.setCurrencyFilter(if (state.currencyFilter == currency) null else currency) },
                            label = { Text(currency.name) })
                    }
                }
            }
            if (state.filtered.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text("No transactions found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            groupOrder.forEach { group ->
                val txs = grouped[group]
                if (!txs.isNullOrEmpty()) {
                    item {
                        Text(group, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    item {
                        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp)) {
                            txs.forEachIndexed { i, tx ->
                                TransactionItem(tx = tx, onClick = { onTransactionClick(tx.id) })
                                if (i < txs.size - 1) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Divider)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}
