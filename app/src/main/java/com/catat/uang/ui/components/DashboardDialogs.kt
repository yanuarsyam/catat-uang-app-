package com.catat.uang.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.catat.uang.data.model.TransactionItem
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ScanChoiceDialog(
    onDismiss: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pindai Struk / Catatan") },
        text = { Text("Pilih sumber gambar:") },
        confirmButton = {
            Button(onClick = onCameraClick) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Kamera")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onGalleryClick) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Galeri")
            }
        }
    )
}

@Composable
fun ApiKeyDialog(
    initialKey: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var keyInput by remember { mutableStateOf(initialKey) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gemini AI Scanner") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Masukkan Gemini API Key untuk pembacaan struk & catatan tulisan tangan super akurat (kosongkan untuk memakai kunci bawaan):",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    label = { Text("AIzaSy...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(keyInput) }) { Text("Simpan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
fun DeleteTransactionDialog(
    item: TransactionItem,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val isTransfer = item.type == "TRANSFER"
    val message = if (isTransfer) {
        "Riwayat transfer ini akan dihapus dan saldo ${item.accountName} serta ${item.targetAccountName ?: "dompet tujuan"} akan dikembalikan ke posisi sebelumnya."
    } else {
        "Catatan '${item.note.ifBlank { item.categoryName }}' akan dihapus dan saldo ${item.accountName} otomatis disesuaikan."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isTransfer) "Hapus Riwayat Transfer?" else "Hapus Transaksi?") },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Hapus") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
fun TransactionRow(
    item: TransactionItem,
    formatter: NumberFormat,
    onEditClick: () -> Unit,
    onViewReceiptClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isExpense = item.type == "EXPENSE"
    val isTransfer = item.type == "TRANSFER"
    val hasReceipt = !item.receiptPath.isNullOrBlank()
    val dateStr = remember(item.timestamp) {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date(item.timestamp))
    }

    val titleText = when {
        isTransfer -> item.note.ifBlank { "Transfer Antar Dompet" }
        else -> item.note.ifBlank { item.categoryName }
    }

    val subtitleText = when {
        isTransfer -> "${item.accountName} → ${item.targetAccountName ?: "Dompet Tujuan"} • $dateStr"
        else -> "${item.categoryName} • ${item.accountName} • $dateStr"
    }

    val amountPrefix = when {
        isTransfer -> "⇄ "
        isExpense -> "-"
        else -> "+"
    }

    val amountColor = when {
        isTransfer -> Color(0xFF1565C0)
        isExpense -> MaterialTheme.colorScheme.error
        else -> Color(0xFF2E7D32)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (!isTransfer) Modifier.clickable { onEditClick() } else Modifier),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
            Text(
                text = "$amountPrefix${formatter.format(item.amount)}",
                style = MaterialTheme.typography.titleMedium,
                color = amountColor
            )
            if (hasReceipt) {
                IconButton(onClick = onViewReceiptClick) {
                    Icon(
                        Icons.Default.ReceiptLong,
                        contentDescription = "Lihat Foto Struk",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            IconButton(onClick = onDeleteClick) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Hapus Transaksi",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
