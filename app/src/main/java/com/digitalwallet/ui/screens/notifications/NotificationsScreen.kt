package com.digitalwallet.ui.screens.notifications

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digitalwallet.data.database.entity.NotificationEntity
import com.digitalwallet.data.database.entity.NotificationType
import com.digitalwallet.domain.repository.NotificationRepository
import com.digitalwallet.ui.components.WalletTopBar
import com.digitalwallet.ui.components.formatDate
import com.digitalwallet.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(private val notificationRepository: NotificationRepository) : ViewModel() {
    private val _notifications = MutableStateFlow<List<NotificationEntity>>(emptyList())
    val notifications: StateFlow<List<NotificationEntity>> = _notifications

    init {
        viewModelScope.launch {
            notificationRepository.getAll().collect { _notifications.value = it }
        }
    }

    fun markAllRead() { viewModelScope.launch { notificationRepository.markAllRead() } }
    fun delete(n: NotificationEntity) { viewModelScope.launch { notificationRepository.delete(n) } }
}

fun notificationIcon(type: NotificationType): ImageVector = when (type) {
    NotificationType.RECEIVED -> Icons.Default.ArrowDownward
    NotificationType.SENT -> Icons.Default.ArrowUpward
    NotificationType.PURCHASED -> Icons.Default.ShoppingCart
    NotificationType.REDEEMED -> Icons.Default.Redeem
    NotificationType.SUBSCRIPTION -> Icons.Default.Subscriptions
    NotificationType.SYSTEM -> Icons.Default.Info
}

fun notificationColor(type: NotificationType): Color = when (type) {
    NotificationType.RECEIVED -> WalletTeal500
    NotificationType.SENT -> WalletRed500
    NotificationType.PURCHASED -> EurBlue
    NotificationType.REDEEMED -> SsdPurple
    NotificationType.SUBSCRIPTION -> WalletGold500
    NotificationType.SYSTEM -> TextSecondary
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(onBack: () -> Unit, viewModel: NotificationsViewModel = hiltViewModel()) {
    val notifications by viewModel.notifications.collectAsState()

    Scaffold(
        topBar = {
            WalletTopBar(title = "Notifications", onBack = onBack, actions = {
                TextButton(onClick = { viewModel.markAllRead() }) {
                    Text("Mark All Read", color = Color.White)
                }
            })
        }
    ) { padding ->
        if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No notifications", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(notifications, key = { it.id }) { notification ->
                    var visible by remember { mutableStateOf(true) }
                    AnimatedVisibility(visible = visible, exit = fadeOut(tween(300)) + shrinkVertically(tween(300))) {
                        SwipeToDismissBox(
                            state = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart) {
                                        visible = false
                                        viewModel.delete(notification)
                                        true
                                    } else false
                                }
                            ),
                            backgroundContent = {
                                Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(WalletRed500),
                                    contentAlignment = Alignment.CenterEnd) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.padding(end = 16.dp))
                                }
                            }
                        ) {
                            NotificationCard(notification = notification)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCard(notification: NotificationEntity) {
    val bgColor by animateColorAsState(
        targetValue = if (!notification.isRead) WalletNavy700.copy(alpha = 0.05f) else Color.Transparent,
        label = "bgColor"
    )
    val icon = notificationIcon(notification.type)
    val iconColor = notificationColor(notification.type)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor.takeIf { !notification.isRead } ?: MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(iconColor.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(notification.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    Text(formatDate(notification.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(notification.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
            if (!notification.isRead) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(WalletNavy700))
            }
        }
    }
}
