package com.catat.uang.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PhotoLibrary
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
                    text = "Masukkan Gemini API Key untuk pembacaan struk & catatan tulisan tangan super akurat (kosongkan untuk mode offline):",
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hapus Transaksi?") },
        text = {
            Text("Catatan '${item.note.ifBlank { item.categoryName }}' akan dihapus dan saldo ${item.accountName} otomatis disesuaikan.")
        },
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
    onDeleteClick: () -> Unit
) {
    val isExpense = item.type == "EXPENSE"
    val dateStr = remember(item.timestamp) {
        SimpleDateFormat("dd MMM, HH:mm", Locale("id", "ID")).format(Date(item.timestamp))
    }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.note.ifBlank { item.categoryName },
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "${item.accountName} • $dateStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
            Text(
                text = "${if (isExpense) "-" else "+"}${formatter.format(item.amount)}",
                style = MaterialTheme.typography.titleMedium,
                color = if (isExpense) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
            )
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
