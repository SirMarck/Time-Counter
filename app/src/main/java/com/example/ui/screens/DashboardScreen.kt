package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.Client
import com.example.data.Session
import com.example.utils.FormatUtils
import com.example.utils.UpdateManager
import com.example.viewmodel.TimeTrackerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

import androidx.compose.material.icons.filled.Settings

import com.example.ui.theme.luxBorder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: TimeTrackerViewModel) {
    val clients by viewModel.clients.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()

    var showStartDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Calculate estimating earnings this month
    var estimatedEarnings by remember { mutableStateOf(0.0) }
    
    LaunchedEffect(sessions, clients) {
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        
        var total = 0.0
        sessions.filter { it.endTime != null }.forEach { session ->
            val cal = Calendar.getInstance().apply { timeInMillis = session.startTime }
            if (cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear) {
                val client = clients.find { it.id == session.clientId }
                if (client != null) {
                    val durationHours = (session.endTime!! - session.startTime).toDouble() / (1000 * 60 * 60)
                    total += durationHours * client.hourlyRate
                }
            }
        }
        estimatedEarnings = total
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard", fontWeight = FontWeight.Bold) },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Configurações")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Procurar Atualizações") },
                                onClick = {
                                    showMenu = false
                                    scope.launch {
                                        val found = UpdateManager.checkForUpdates(context)
                                        if (!found) {
                                            Toast.makeText(context, "Nenhuma atualização encontrada.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Update, contentDescription = null)
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (activeSession == null && clients.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showStartDialog = true },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Iniciar") },
                    text = { Text("Iniciar Trabalho") }
                )
            } else if (activeSession != null) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.stopActiveSession() },
                    icon = { Icon(Icons.Default.Stop, contentDescription = "Parar") },
                    text = { Text("Parar Tempo") },
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .luxBorder(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "GANHOS ESTIMADOS (MÊS ATUAL)", 
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = FormatUtils.formatCurrency(estimatedEarnings),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            if (activeSession != null) {
                ActiveSessionCard(activeSession!!, clients)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Trabalhos Recentes", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            val completedSessions = sessions.filter { it.endTime != null }.take(10)
            if (completedSessions.isEmpty()) {
                Text("Nenhum trabalho finalizado ainda.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(completedSessions) { session ->
                        val client = clients.find { it.id == session.clientId }
                        SessionItem(session, client)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        if (showStartDialog) {
            StartSessionDialog(
                clients = clients,
                onDismiss = { showStartDialog = false },
                onStart = { clientId, desc ->
                    viewModel.startSession(clientId, desc)
                    showStartDialog = false
                }
            )
        }
    }
}

@Composable
fun ActiveSessionCard(session: Session, clients: List<Client>) {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = System.currentTimeMillis()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .luxBorder(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        val client = clients.find { it.id == session.clientId }
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Timer Ativo", 
                    style = MaterialTheme.typography.labelMedium, 
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (client != null) {
                    Text(client.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Text(
                    session.description, 
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            
            val duration = currentTime - session.startTime
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = FormatUtils.formatDuration(duration),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun SessionItem(session: Session, client: Client?) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .luxBorder(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(client?.name ?: "Cliente Desconhecido", fontWeight = FontWeight.Bold)
                Text(FormatUtils.formatDate(session.startTime))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(session.description, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            val duration = if (session.endTime != null) session.endTime - session.startTime else 0L
            val value = if (client != null) {
                (duration.toDouble() / (1000 * 60 * 60)) * client.hourlyRate
            } else 0.0
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Duração: ${FormatUtils.formatDuration(duration)}", style = MaterialTheme.typography.bodySmall)
                Text(FormatUtils.formatCurrency(value), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun StartSessionDialog(
    clients: List<Client>,
    onDismiss: () -> Unit,
    onStart: (Long, String) -> Unit
) {
    if (clients.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Nenhum cliente") },
            text = { Text("Adicione um cliente primeiro na aba Clientes.") },
            confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
        )
        return
    }

    var selectedClientId by remember { mutableStateOf(clients.first().id) }
    var description by remember { mutableStateOf("") }
    
    // In a real app we'd use a Dropdown or similar. For simplicity, just next/prev or a simple list.
    // Let's use a very simple setup.
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Iniciar Trabalho") },
        text = {
            Column {
                Text("Selecione o Cliente:")
                clients.forEach { client ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = client.id == selectedClientId,
                            onClick = { selectedClientId = client.id }
                        )
                        Text(client.name)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição do Serviço") }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onStart(selectedClientId, description) }) {
                Text("Iniciar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
