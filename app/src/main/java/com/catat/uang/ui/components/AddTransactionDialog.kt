package com.catat.uang.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarMonth
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
import com.catat.uang.data.model.TransactionItem
import com.catat.uang.util.ReceiptStorage
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    accounts: List<Account>,
    categories: List<Category>,
    editingItem: TransactionItem? = null,
    initialAmount: Long = 0L,
    initialNote: String = "",
    initialCategoryName: String? = null,
    initialReceiptPath: String? = null,
    onDismiss: () -> Unit,
    onSave: (accountId: Long, categoryId: Long, amount: Long, type: String, note: String, timestamp: Long, receiptPath: String?) -> Unit
) {
    val context = LocalContext.current
    val isEdit = editingItem != null

    var type by remember { mutableStateOf(editingItem?.type ?: "EXPENSE") }
    var amountText by remember {
        mutableStateOf(
            when {
                editingItem != null -> editingItem.amount.toString()
                initialAmount > 0L -> initialAmount.toString()
                else -> ""
            }
        )
    }
    var note by remember { mutableStateOf(editingItem?.note ?: initialNote) }
    var receiptPath by remember { mutableStateOf(editingItem?.receiptPath ?: initialReceiptPath) }
    var selectedTimestamp by remember { mutableStateOf(editingItem?.timestamp ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    var selectedAccount by remember(accounts) {
        mutableStateOf(accounts.find { it.id == editingItem?.accountId } ?: accounts.firstOrNull())
    }
    var selectedCategory by remember(categories) {
        mutableStateOf(
            categories.find { it.id == editingItem?.categoryId }
                ?: initialCategoryName?.let { target -> categories.find { it.name.equals(target, ignoreCase = true) } }
                ?: categories.firstOrNull()
        )
    }
    var accExpanded by remember { mutableStateOf(false) }
    var catExpanded by remember { mutableStateOf(false) }

    val dateLabel = remember(selectedTimestamp) {
        SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(Date(selectedTimestamp))
    }

    val attachLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            if (!isEdit && receiptPath != null) ReceiptStorage.deleteReceiptFile(receiptPath)
            receiptPath = ReceiptStorage.saveReceiptFromUri(context, uri)
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = selectedTimestamp)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { pickedUtc ->
                            selectedTimestamp = mergeLocalDateWithTime(pickedUtc, selectedTimestamp)
                        }
                        showDatePicker = false
                    }
                ) { Text("Pilih") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Batal") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!isEdit && receiptPath != null && receiptPath != initialReceiptPath) {
                ReceiptStorage.deleteReceiptFile(receiptPath)
            }
            onDismiss()
        },
        title = { Text(if (isEdit) "Edit Transaksi" else "Catat Transaksi") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = type == "EXPENSE",
                            onClick = { type = "EXPENSE" },
                            label = { Text("Keluar") }
                        )
                        FilterChip(
                            selected = type == "INCOME",
                            onClick = { type = "INCOME" },
                            label = { Text("Masuk") }
                        )
                    }
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(dateLabel, style = MaterialTheme.typography.labelMedium)
                    }
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
                                    if (!isEdit) ReceiptStorage.deleteReceiptFile(receiptPath)
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
                        onSave(acc.id, cat.id, amt, type, note.trim(), selectedTimestamp, receiptPath)
                        onDismiss()
                    }
                }
            ) { Text(if (isEdit) "Simpan Perubahan" else "Simpan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

private fun mergeLocalDateWithTime(pickedUtcMillis: Long, originalTimestamp: Long): Long {
    val zone = ZoneId.systemDefault()
    val pickedDate = Instant.ofEpochMilli(pickedUtcMillis).atZone(ZoneOffset.UTC).toLocalDate()
    val originalTime = Instant.ofEpochMilli(originalTimestamp).atZone(zone).toLocalTime()
    return pickedDate.atTime(originalTime).atZone(zone).toInstant().toEpochMilli()
}
