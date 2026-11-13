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
fun EditAccountDialog(
    account: Account,
    onDismiss: () -> Unit,
    onSave: (newName: String, newBalance: Long) -> Unit
) {
    var nameText by remember { mutableStateOf(account.name) }
    var balanceText by remember { mutableStateOf(account.balance.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ubah Akun / Dompet") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Nama Bank / E-Wallet") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { if (it.all { c -> c.isDigit() }) balanceText = it },
                    label = { Text("Saldo Saat Ini (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameText.isNotBlank()) {
                        val newBal = balanceText.toLongOrNull() ?: 0L
                        onSave(nameText, newBal)
                        onDismiss()
                    }
                }
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
