package com.digitalwallet.ui.screens.marketplace

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalwallet.data.database.entity.CurrencyType
import com.digitalwallet.ui.components.WalletTopBar
import com.digitalwallet.ui.components.formatCurrency
import com.digitalwallet.ui.theme.*

@Composable
fun MarketplaceScreen(
    onBack: () -> Unit,
    onNavigateToBuy: () -> Unit,
    onNavigateToSell: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val tokens = listOf(
        Triple(CurrencyType.USD, "100.0 Tokens / USD", "+2.3%"),
        Triple(CurrencyType.EUR, "110.0 Tokens / USD", "+1.1%"),
        Triple(CurrencyType.SSD, "50.0 Tokens / USD", "+5.7%")
    )

    Scaffold(topBar = { WalletTopBar(title = "Marketplace", onBack = onBack) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Buy") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Sell") })
            }
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { Spacer(modifier = Modifier.height(4.dp)) }
                if (selectedTab == 0) {
                    item {
                        Text("Available Token Types", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    items(tokens.size) { i ->
                        val (currency, rate, change) = tokens[i]
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(currency.displayName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(rate, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(change, color = WalletTeal500, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Button(
                                    onClick = onNavigateToBuy,
                                    colors = ButtonDefaults.buttonColors(containerColor = WalletNavy700),
                                    shape = RoundedCornerShape(8.dp)
                                ) { Text("Buy", color = Color.White, fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                } else {
                    item {
                        Text("Sell Your Tokens", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    items(tokens.size) { i ->
                        val (currency, rate, _) = tokens[i]
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(currency.displayName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(rate, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Button(
                                    onClick = onNavigateToSell,
                                    colors = ButtonDefaults.buttonColors(containerColor = WalletGold500),
                                    shape = RoundedCornerShape(8.dp)
                                ) { Text("Sell", color = WalletNavy900, fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                }
            }
        }
    }
}
