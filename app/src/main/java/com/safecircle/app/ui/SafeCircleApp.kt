package com.safecircle.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun SafeCircleApp(vm: SafeCircleViewModel) {
    var tab by remember { mutableIntStateOf(0) }
    MaterialTheme(colorScheme = lightColorScheme(primary = androidx.compose.ui.graphics.Color(0xFF9B1C1C), surface = androidx.compose.ui.graphics.Color(0xFFFAF8FF))) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(tab == 0, { tab = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("SOS") })
                    NavigationBarItem(tab == 1, { tab = 1 }, icon = { Icon(Icons.Default.Contacts, null) }, label = { Text("Contacts") })
                    NavigationBarItem(tab == 2, { tab = 2 }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Settings") })
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when (tab) {
                    0 -> HomeScreen(vm)
                    1 -> ContactsScreen(vm)
                    else -> SettingsScreen(vm)
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(vm: SafeCircleViewModel) {
    val peers by vm.peerCount.collectAsState()
    val status by vm.lastStatus.collectAsState()
    var showCountdown by remember { mutableStateOf(false) }

    if (showCountdown) {
        SosCountdown(onCancel = { showCountdown = false }, onConfirm = { showCountdown = false; vm.triggerSos() })
        return
    }

    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(18.dp))
        Text("SafeCircle", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Your small emergency network", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))

        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(16.dp)) {
                Text("Nearby circle", fontWeight = FontWeight.SemiBold)
                Text(if (peers == 0) "No nearby SafeCircle devices" else "$peers nearby circle device${if (peers == 1) "" else "s"} connected")
            }
        }
        Spacer(Modifier.height(28.dp))

        Button(
            onClick = { showCountdown = true },
            modifier = Modifier.size(220.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SOS", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
                Text("Press for help")
            }
        }
        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = vm::callEmergency, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Call, null)
            Spacer(Modifier.width(8.dp))
            Text("Call emergency services")
        }
        Spacer(Modifier.height(12.dp))
        Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        Text("Supplemental safety tool. It does not replace emergency services or your phone's built-in Emergency SOS.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SosCountdown(onCancel: () -> Unit, onConfirm: () -> Unit) {
    var seconds by remember { mutableIntStateOf(5) }
    LaunchedEffect(Unit) {
        while (seconds > 0) { delay(1000); seconds-- }
        onConfirm()
    }
    Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("SOS is about to send", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text("$seconds", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error)
        Text("Your location will be shared with configured contacts and nearby SafeCircle devices.")
        Spacer(Modifier.height(28.dp))
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}

@Composable
private fun ContactsScreen(vm: SafeCircleViewModel) {
    val saved by vm.contacts.collectAsState()
    var editing by remember { mutableStateOf<Int?>(null) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Emergency contacts", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Up to 3 people can receive an SOS by SMS when cellular service is available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(3) { index ->
                val contact = saved.getOrNull(index)
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(contact?.name ?: "Contact ${index + 1}", fontWeight = FontWeight.SemiBold)
                            Text(contact?.phone ?: "Not configured", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = {
                            editing = index
                            name = contact?.name ?: ""
                            phone = contact?.phone ?: ""
                        }) { Text(if (contact == null) "Add" else "Edit") }
                    }
                }
            }
        }
    }

    if (editing != null) {
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Emergency contact") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                    OutlinedTextField(phone, { phone = it }, label = { Text("Phone number") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                }
            },
            confirmButton = { TextButton(onClick = { vm.saveContact(editing!!, name, phone); editing = null }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SettingsScreen(vm: SafeCircleViewModel) {
    val nameValue by vm.name.collectAsState()
    val codeValue by vm.circleCode.collectAsState()
    val numberValue by vm.emergencyNumber.collectAsState()
    var name by remember(nameValue) { mutableStateOf(nameValue) }
    var code by remember(codeValue) { mutableStateOf(codeValue) }
    var emergencyNumber by remember(numberValue) { mutableStateOf(numberValue) }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Use the same circle code on the three phones. It helps prevent unrelated nearby SafeCircle users from being treated as your circle.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(name, { name = it }, label = { Text("Your name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(code, { code = it.filter(Char::isDigit).take(8) }, label = { Text("Circle code") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(emergencyNumber, { emergencyNumber = it }, label = { Text("Emergency number") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
        Spacer(Modifier.height(20.dp))
        Button(onClick = { vm.saveProfile(name, code, emergencyNumber) }, modifier = Modifier.fillMaxWidth()) { Text("Save settings") }
        Spacer(Modifier.height(24.dp))
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Offline behavior", fontWeight = FontWeight.Bold)
                Text("Nearby SafeCircle phones can exchange SOS data without internet. SMS still needs cellular service. A normal Android app cannot send an off-grid SOS to a remote person without some communication path.")
            }
        }
    }
}
