package com.catat.uang.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.catat.uang.data.model.Account

@Composable
fun EditBalanceDialog(
    account: Account,
    onDismiss: () -> Unit,
    onSave: (newBalance: Long) -> Unit
) {
    var balanceText by remember { mutableStateOf(account.balance.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Atur Saldo ${account.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Ketik nominal saldo saat ini untuk dompet ini:",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { if (it.all { c -> c.isDigit() }) balanceText = it },
                    label = { Text("Nominal Saldo (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newBal = balanceText.toLongOrNull() ?: 0L
                    onSave(newBal)
                    onDismiss()
                }
            ) {
                Text("Simpan Saldo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
