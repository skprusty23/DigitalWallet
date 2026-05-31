package com.digitalwallet.ui.screens.contacts

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digitalwallet.data.database.entity.ContactEntity
import com.digitalwallet.data.database.entity.CurrencyType
import com.digitalwallet.domain.repository.ContactRepository
import com.digitalwallet.ui.components.WalletTopBar
import com.digitalwallet.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ContactsState(
    val contacts: List<ContactEntity> = emptyList(),
    val favorites: List<ContactEntity> = emptyList(),
    val searchQuery: String = "",
    val showAddDialog: Boolean = false,
    val newName: String = "",
    val newEmail: String = "",
    val newAddress: String = ""
)

@HiltViewModel
class ContactsViewModel @Inject constructor(private val contactRepository: ContactRepository) : ViewModel() {
    private val _state = MutableStateFlow(ContactsState())
    val state: StateFlow<ContactsState> = _state

    init {
        viewModelScope.launch {
            contactRepository.getAll().collect { contacts ->
                _state.value = _state.value.copy(
                    contacts = contacts,
                    favorites = contacts.filter { it.isFavorite }
                )
            }
        }
    }

    fun setSearch(q: String) {
        _state.value = _state.value.copy(searchQuery = q)
        viewModelScope.launch {
            if (q.isBlank()) {
                contactRepository.getAll().collect { contacts ->
                    _state.value = _state.value.copy(contacts = contacts, favorites = contacts.filter { it.isFavorite })
                }
            } else {
                contactRepository.search(q).collect { contacts ->
                    _state.value = _state.value.copy(contacts = contacts, favorites = contacts.filter { it.isFavorite })
                }
            }
        }
    }

    fun toggleFavorite(contact: ContactEntity) {
        viewModelScope.launch { contactRepository.update(contact.copy(isFavorite = !contact.isFavorite)) }
    }

    fun showDialog() { _state.value = _state.value.copy(showAddDialog = true) }
    fun hideDialog() { _state.value = _state.value.copy(showAddDialog = false, newName = "", newEmail = "", newAddress = "") }
    fun setNewName(v: String) { _state.value = _state.value.copy(newName = v) }
    fun setNewEmail(v: String) { _state.value = _state.value.copy(newEmail = v) }
    fun setNewAddress(v: String) { _state.value = _state.value.copy(newAddress = v) }

    fun addContact() {
        val s = _state.value
        if (s.newName.isBlank()) return
        viewModelScope.launch {
            val avatarColors = listOf(0xFF1565C0L, 0xFF2E7D32L, 0xFF6A1B9AL, 0xFF00838FL, 0xFFBF360CL)
            contactRepository.insert(ContactEntity(
                name = s.newName.trim(),
                email = s.newEmail.trim(),
                walletAddress = s.newAddress.ifBlank { UUID.randomUUID().toString() },
                preferredCurrency = CurrencyType.USD,
                avatarColor = avatarColors.random()
            ))
            hideDialog()
        }
    }

    fun delete(contact: ContactEntity) {
        viewModelScope.launch { contactRepository.delete(contact) }
    }
}

@Composable
fun FrequentContactsScreen(onBack: () -> Unit, viewModel: ContactsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    if (state.showAddDialog) {
        AddContactDialog(state = state, onDismiss = { viewModel.hideDialog() }, onConfirm = { viewModel.addContact() },
            onNameChange = { viewModel.setNewName(it) }, onEmailChange = { viewModel.setNewEmail(it) }, onAddressChange = { viewModel.setNewAddress(it) })
    }

    Scaffold(
        topBar = { WalletTopBar(title = "Contacts", onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showDialog() }, containerColor = WalletNavy700) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add Contact", tint = Color.White)
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.setSearch(it) },
                    label = { Text("Search contacts") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
            }
            if (state.favorites.isNotEmpty()) {
                item { Text("Favorites", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = WalletGold500) }
                items(state.favorites) { contact ->
                    ContactCardFull(contact = contact, onFavoriteToggle = { viewModel.toggleFavorite(contact) }, onDelete = { viewModel.delete(contact) })
                }
                item { HorizontalDivider(color = Divider) }
                item { Text("All Contacts", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
            }
            items(state.contacts.filter { !it.isFavorite }) { contact ->
                ContactCardFull(contact = contact, onFavoriteToggle = { viewModel.toggleFavorite(contact) }, onDelete = { viewModel.delete(contact) })
            }
            if (state.contacts.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No contacts yet. Add one!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun ContactCardFull(contact: ContactEntity, onFavoriteToggle: () -> Unit, onDelete: () -> Unit) {
    val initials = contact.name.split(" ").take(2).map { it.firstOrNull()?.uppercaseChar() ?: ' ' }.joinToString("")
    val bgColor = Color(contact.avatarColor)

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(bgColor), contentAlignment = Alignment.Center) {
                Text(initials, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(contact.name, fontWeight = FontWeight.SemiBold)
                if (contact.email.isNotBlank()) Text(contact.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(contact.walletAddress.take(20) + "...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                contact.lastTransactionAt?.let { Text("Last: ${it.toLocalDate()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            IconButton(onClick = onFavoriteToggle) {
                Icon(if (contact.isFavorite) Icons.Default.Star else Icons.Default.StarBorder, contentDescription = "Favorite",
                    tint = if (contact.isFavorite) WalletGold500 else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = WalletRed500)
            }
        }
    }
}

@Composable
fun AddContactDialog(
    state: ContactsState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onAddressChange: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Contact", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = state.newName, onValueChange = onNameChange, label = { Text("Name *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = state.newEmail, onValueChange = onEmailChange, label = { Text("Email (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = state.newAddress, onValueChange = onAddressChange, label = { Text("Wallet Address (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text("Add Contact") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
