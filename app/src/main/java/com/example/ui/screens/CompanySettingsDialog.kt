package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CompanySettingsDialog(
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("time_tracker_prefs", Context.MODE_PRIVATE) }
    
    var companyName by remember { mutableStateOf(sharedPrefs.getString("company_name", "") ?: "") }
    var companyCnpj by remember { mutableStateOf(sharedPrefs.getString("company_cnpj", "") ?: "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configurações da Empresa") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Insira os dados da sua empresa para que sejam impressos automaticamente no rodapé de cada relatório exportado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = { Text("Nome da Empresa") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = companyCnpj,
                    onValueChange = { companyCnpj = it },
                    label = { Text("CNPJ") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    sharedPrefs.edit()
                        .putString("company_name", companyName)
                        .putString("company_cnpj", companyCnpj)
                        .apply()
                    onDismiss()
                }
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
