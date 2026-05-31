package com.digitalwallet.ui.screens.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digitalwallet.data.database.entity.*
import com.digitalwallet.domain.repository.*
import com.digitalwallet.security.PinManager
import com.digitalwallet.ui.components.*
import com.digitalwallet.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardState(
    val userName: String = "Shridhar",
    val wallets: List<WalletEntity> = emptyList(),
    val totalBalance: Double = 0.0,
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val unreadNotifications: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    private val transactionRepository: TransactionRepository,
    private val notificationRepository: NotificationRepository,
    private val pinManager: PinManager
) : ViewModel() {
    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state

    init {
        viewModelScope.launch {
            val name = pinManager.getUserName()
            _state.update { it.copy(userName = name) }
        }
        viewModelScope.launch {
            combine(
                walletRepository.getAllWallets(),
                walletRepository.getTotalBalance(),
                transactionRepository.getRecent(5),
                notificationRepository.getUnreadCount()
            ) { wallets, total, txs, unread ->
                DashboardState(
                    userName = _state.value.userName,
                    wallets = wallets,
                    totalBalance = total ?: 0.0,
                    recentTransactions = txs,
                    unreadNotifications = unread,
                    isLoading = false
                )
            }.collect { _state.value = it }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToWallets: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSend: () -> Unit,
    onNavigateToReceive: () -> Unit,
    onNavigateToBuy: () -> Unit,
    onNavigateToSell: () -> Unit,
    onNavigateToQrScan: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onWalletClick: (Long) -> Unit,
    onTransactionClick: (Long) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        Triple("Home", Icons.Default.Home, {}),
        Triple("Wallets", Icons.Default.AccountBalanceWallet, onNavigateToWallets),
        Triple("History", Icons.Default.History, onNavigateToTransactions),
        Triple("Reports", Icons.Default.BarChart, onNavigateToReports),
        Triple("More", Icons.Default.MoreHoriz, onNavigateToSettings)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Good day,", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        Text(state.userName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Lock, contentDescription = "Lock", tint = Color.White)
                    }
                    BadgedBox(badge = {
                        if (state.unreadNotifications > 0) {
                            Badge { Text(state.unreadNotifications.toString()) }
                        }
                    }) {
                        IconButton(onClick = onNavigateToNotifications) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WalletNavy700)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                tabs.forEachIndexed { index, (label, icon, action) ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            if (index != 0) action()
                        },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, fontSize = 10.sp) }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Portfolio Total Card
            TotalPortfolioCard(totalBalance = state.totalBalance, walletCount = state.wallets.size)
            Spacer(modifier = Modifier.height(16.dp))

            // Wallet Cards
            if (state.wallets.isNotEmpty()) {
                SectionHeader(title = "My Wallets", actionLabel = "See All", onAction = onNavigateToWallets)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    state.wallets.forEach { wallet ->
                        WalletCard(wallet = wallet, onClick = { onWalletClick(wallet.id) })
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Quick Actions
            SectionHeader(title = "Quick Actions")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                QuickActionButton(Icons.Default.ArrowUpward, "Send", onNavigateToSend, WalletRed500)
                QuickActionButton(Icons.Default.ArrowDownward, "Receive", onNavigateToReceive, WalletTeal500)
                QuickActionButton(Icons.Default.ShoppingCart, "Buy", onNavigateToBuy, WalletNavy700)
                QuickActionButton(Icons.Default.TrendingUp, "Sell", onNavigateToSell, WalletGold500)
                QuickActionButton(Icons.Default.QrCodeScanner, "Scan QR", onNavigateToQrScan, WalletTeal400)
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Recent Transactions
            SectionHeader(title = "Recent Transactions", actionLabel = "View All", onAction = onNavigateToTransactions)
            if (state.recentTransactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No transactions yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    state.recentTransactions.forEach { tx ->
                        TransactionItem(tx = tx, onClick = { onTransactionClick(tx.id) })
                        if (tx != state.recentTransactions.last()) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Divider)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Market Overview
            MarketOverviewCard()
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun TotalPortfolioCard(totalBalance: Double, walletCount: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerX by infiniteTransition.animateFloat(
        initialValue = -300f, targetValue = 900f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "shimmerX"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(WalletNavy900, WalletNavy800, WalletNavy700)))
    ) {
        // shimmer overlay
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.linearGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.05f), Color.Transparent),
                    start = Offset(shimmerX, 0f), end = Offset(shimmerX + 300f, 200f)
                )
            )
        )
        Column(modifier = Modifier.padding(20.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total Portfolio", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                Surface(shape = RoundedCornerShape(8.dp), color = WalletGold500.copy(alpha = 0.2f)) {
                    Text("$walletCount Wallets", color = WalletGold400, style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
            Text(
                text = "$ ${formatCurrency(totalBalance)}",
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold
            )
            Text("Across all currencies", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
        }
    }
}

@Composable
fun MarketOverviewCard() {
    val tokens = listOf(
        Triple("USD Token", "+2.3%", Color(0xFF00C853)),
        Triple("EUR Token", "+1.1%", Color(0xFF2979FF)),
        Triple("SSD Token", "+5.7%", Color(0xFFAA00FF))
    )
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Market Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            tokens.forEach { (name, change, color) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(name, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(change, color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
