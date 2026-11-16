package com.catat.uang.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.catat.uang.data.model.Account
import com.catat.uang.data.model.Category
import com.catat.uang.util.ParsedReceipt
import com.catat.uang.util.ParsedReceiptItem
import java.text.NumberFormat

data class SplitItemSelection(
    val item: ParsedReceiptItem,
    val selected: Boolean,
    val categoryId: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiItemScanDialog(
    parsedReceipt: ParsedReceipt,
    accounts: List<Account>,
    categories: List<Category>,
    formatter: NumberFormat,
    onDismiss: () -> Unit,
    onSaveSingleTotal: () -> Unit,
    onSaveSplitItems: (accountId: Long, chosenItems: List<Pair<ParsedReceiptItem, Long>>) -> Unit
) {
    var selectedAccount by remember(accounts) { mutableStateOf(accounts.firstOrNull()) }
    var accExpanded by remember { mutableStateOf(false) }

    val defaultCatId = categories.firstOrNull()?.id ?: 1L
    val selections = remember(parsedReceipt, categories) {
        mutableStateListOf<SplitItemSelection>().apply {
            parsedReceipt.items.forEach { item ->
                val matchedCat = categories.find { it.name.equals(item.suggestedCategory, ignoreCase = true) }
                add(SplitItemSelection(item, true, matchedCat?.id ?: defaultCatId))
            }
        }
    }

    val totalSelectedAmount = selections.filter { it.selected }.sumOf { it.item.amount }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Terdeteksi ${parsedReceipt.items.size} Item Struk") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Toko: ${parsedReceipt.merchant} • Total: ${formatter.format(parsedReceipt.totalAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )

                ExposedDropdownMenuBox(
                    expanded = accExpanded,
                    onExpandedChange = { accExpanded = !accExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedAccount?.name ?: "Pilih Dompet",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Bayar Pakai Dompet") },
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

                LazyColumn(
                    modifier = Modifier.heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(selections) { idx, entry ->
                        var catMenuOpen by remember { mutableStateOf(false) }
                        val currentCat = categories.find { it.id == entry.categoryId } ?: categories.firstOrNull()

                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = entry.selected,
                                    onCheckedChange = { checked ->
                                        selections[idx] = entry.copy(selected = checked)
                                    }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.item.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text(formatter.format(entry.item.amount), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                }
                                Box {
                                    AssistChip(
                                        onClick = { catMenuOpen = true },
                                        label = { Text(currentCat?.name ?: "Kategori", style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                                    )
                                    DropdownMenu(
                                        expanded = catMenuOpen,
                                        onDismissRequest = { catMenuOpen = false }
                                    ) {
                                        categories.forEach { cat ->
                                            DropdownMenuItem(
                                                text = { Text(cat.name) },
                                                onClick = {
                                                    selections[idx] = entry.copy(categoryId = cat.id)
                                                    catMenuOpen = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "Terpilih (${selections.count { it.selected }} item): ${formatter.format(totalSelectedAmount)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF1565C0)
                )
            }
        },
        confirmButton = {
            Button(
                enabled = selections.any { it.selected } && selectedAccount != null,
                onClick = {
                    val acc = selectedAccount ?: return@Button
                    val chosen = selections.filter { it.selected }.map { it.item to it.categoryId }
                    onSaveSplitItems(acc.id, chosen)
                    onDismiss()
                }
            ) { Text("Pecah & Simpan") }
        },
        dismissButton = {
            TextButton(onClick = onSaveSingleTotal) {
                Text("Gabung 1 Total")
            }
        }
    )
}
