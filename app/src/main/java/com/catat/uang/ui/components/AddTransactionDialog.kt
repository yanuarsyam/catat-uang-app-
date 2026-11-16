package com.catat.uang.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.catat.uang.data.model.Account
import com.catat.uang.data.model.Category
import com.catat.uang.util.ReceiptStorage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    accounts: List<Account>,
    categories: List<Category>,
    initialAmount: Long = 0L,
    initialNote: String = "",
    initialReceiptPath: String? = null,
    onDismiss: () -> Unit,
    onSave: (accountId: Long, categoryId: Long, amount: Long, type: String, note: String, receiptPath: String?) -> Unit
) {
    val context = LocalContext.current
    var type by remember { mutableStateOf("EXPENSE") }
    var amountText by remember { mutableStateOf(if (initialAmount > 0L) initialAmount.toString() else "") }
    var note by remember { mutableStateOf(initialNote) }
    var receiptPath by remember { mutableStateOf(initialReceiptPath) }

    var selectedAccount by remember(accounts) { mutableStateOf(accounts.firstOrNull()) }
    var selectedCategory by remember(categories) { mutableStateOf(categories.firstOrNull()) }
    var accExpanded by remember { mutableStateOf(false) }
    var catExpanded by remember { mutableStateOf(false) }

    val attachLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            ReceiptStorage.deleteReceiptFile(receiptPath)
            receiptPath = ReceiptStorage.saveReceiptFromUri(context, uri)
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (receiptPath != null) ReceiptStorage.deleteReceiptFile(receiptPath)
            onDismiss()
        },
        title = { Text("Catat Transaksi") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == "EXPENSE",
                        onClick = { type = "EXPENSE" },
                        label = { Text("Pengeluaran") }
                    )
                    FilterChip(
                        selected = type == "INCOME",
                        onClick = { type = "INCOME" },
                        label = { Text("Pemasukan") }
                    )
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { if (it.all { c -> c.isDigit() }) amountText = it },
                    label = { Text("Nominal (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = accExpanded,
                    onExpandedChange = { accExpanded = !accExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedAccount?.name ?: "Pilih Dompet",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Dompet / Rekening") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = accExpanded,
                        onDismissRequest = { accExpanded = false }
                    ) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text(acc.name) },
                                onClick = { selectedAccount = acc; accExpanded = false }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = catExpanded,
                    onExpandedChange = { catExpanded = !catExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.name ?: "Pilih Kategori",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kategori") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = catExpanded,
                        onDismissRequest = { catExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = { selectedCategory = cat; catExpanded = false }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Keterangan / Nama Toko") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (!receiptPath.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Foto struk terarsip", style = MaterialTheme.typography.labelMedium)
                            }
                            IconButton(
                                onClick = {
                                    ReceiptStorage.deleteReceiptFile(receiptPath)
                                    receiptPath = null
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Hapus Lampiran", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { attachLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Lampirkan Foto Struk (Opsional)")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toLongOrNull() ?: 0L
                    val acc = selectedAccount
                    val cat = selectedCategory
                    if (amt > 0L && acc != null && cat != null) {
                        onSave(acc.id, cat.id, amt, type, note.trim(), receiptPath)
                        onDismiss()
                    }
                }
            ) { Text("Simpan") }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (receiptPath != null) ReceiptStorage.deleteReceiptFile(receiptPath)
                    onDismiss()
                }
            ) { Text("Batal") }
        }
    )
}
