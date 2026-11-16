package com.catat.uang.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.catat.uang.data.model.Account
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferDialog(
    accounts: List<Account>,
    formatter: NumberFormat,
    onDismiss: () -> Unit,
    onTransfer: (fromAccountId: Long, toAccountId: Long, amount: Long, note: String) -> Unit
) {
    var fromAccount by remember(accounts) { mutableStateOf(accounts.firstOrNull()) }
    var toAccount by remember(accounts) { mutableStateOf(accounts.getOrNull(1) ?: accounts.firstOrNull()) }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var fromExpanded by remember { mutableStateOf(false) }
    var toExpanded by remember { mutableStateOf(false) }

    val amountVal = amountText.toLongOrNull() ?: 0L
    val isSameAccount = fromAccount != null && toAccount != null && fromAccount?.id == toAccount?.id
    val hasEnoughAccounts = accounts.size >= 2
    val exceedsBalance = fromAccount != null && amountVal > (fromAccount?.balance ?: 0L)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transfer Antar Dompet") },
        text = {
            if (!hasEnoughAccounts) {
                Text(
                    text = "Kamu memerlukan minimal 2 dompet atau rekening untuk melakukan transfer saldo.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = fromExpanded,
                        onExpandedChange = { fromExpanded = !fromExpanded }
                    ) {
                        OutlinedTextField(
                            value = fromAccount?.let { "${it.name} (${formatter.format(it.balance)})" } ?: "Pilih Asal",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Dari Dompet / Rekening") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = fromExpanded,
                            onDismissRequest = { fromExpanded = false }
                        ) {
                            accounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text("${acc.name} (${formatter.format(acc.balance)})") },
                                    onClick = {
                                        fromAccount = acc
                                        fromExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        FilledTonalIconButton(
                            onClick = {
                                val temp = fromAccount
                                fromAccount = toAccount
                                toAccount = temp
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.SwapVert, contentDescription = "Tukar Dompet Asal & Tujuan")
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = toExpanded,
                        onExpandedChange = { toExpanded = !toExpanded }
                    ) {
                        OutlinedTextField(
                            value = toAccount?.let { "${it.name} (${formatter.format(it.balance)})" } ?: "Pilih Tujuan",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Ke Dompet / Rekening") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = toExpanded,
                            onDismissRequest = { toExpanded = false }
                        ) {
                            accounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text("${acc.name} (${formatter.format(acc.balance)})") },
                                    onClick = {
                                        toAccount = acc
                                        toExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (isSameAccount) {
                        Text(
                            text = "Pilih dompet tujuan yang berbeda dari dompet asal.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { if (it.all { c -> c.isDigit() }) amountText = it },
                        label = { Text("Nominal Transfer (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (exceedsBalance && amountVal > 0L) {
                        Text(
                            text = "Info: Nominal melebihi saldo ${fromAccount?.name} saat ini.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFE65100)
                        )
                    }

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Catatan (Opsional)") },
                        placeholder = { Text("Contoh: Tarik tunai ATM / Top up") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (hasEnoughAccounts) {
                Button(
                    enabled = amountVal > 0L && !isSameAccount && fromAccount != null && toAccount != null,
                    onClick = {
                        val src = fromAccount
                        val dst = toAccount
                        if (src != null && dst != null && src.id != dst.id && amountVal > 0L) {
                            onTransfer(src.id, dst.id, amountVal, note)
                            onDismiss()
                        }
                    }
                ) { Text("Transfer") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
