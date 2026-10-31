package com.frequency.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.frequency.android.contacts.UsernameContact
import com.frequency.android.contacts.UsernameContactStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsernameContactsSheet(
    isPresented: Boolean,
    onDismiss: () -> Unit,
    viewModel: ChatViewModel
) {
    if (!isPresented) return
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { UsernameContactStore(context) }
    var username by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var contacts by remember { mutableStateOf(store.contacts()) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text("Direct messages", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text("Enter a saved username or paste the other person's Frequency key.", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(username, { username = it }, Modifier.fillMaxWidth(), label = { Text("Username") }, singleLine = true)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(key, { key = it }, Modifier.fillMaxWidth(), label = { Text("Frequency key (npub or hex)") }, minLines = 1, maxLines = 2)
            if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    val targetKey = key.trim().ifBlank { store.find(username)?.key.orEmpty() }
                    val peerId = viewModel.openFrequencyKey(targetKey)
                    if (peerId != null) {
                        viewModel.startPrivateChat(peerId)
                        onDismiss()
                    } else message = "Add a saved username or a valid Frequency key."
                }, modifier = Modifier.weight(1f)) { Text("Message") }
                OutlinedButton(onClick = {
                    try {
                        store.save(username, key)
                        contacts = store.contacts()
                        message = "Contact saved on this device"
                    } catch (e: Exception) { message = e.message ?: "Could not save contact" }
                }, modifier = Modifier.weight(1f)) { Text("Save") }
            }
            Spacer(Modifier.height(18.dp))
            Text("Saved contacts", style = MaterialTheme.typography.titleMedium)
            LazyColumn(Modifier.heightIn(max = 240.dp)) {
                items(contacts) { contact ->
                    ContactRow(contact) {
                        username = contact.username
                        key = contact.key
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactRow(contact: UsernameContact, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp)) {
        Text("@${contact.username}", style = MaterialTheme.typography.bodyLarge)
        Text(contact.key.take(24) + "…", style = MaterialTheme.typography.bodySmall)
    }
}
