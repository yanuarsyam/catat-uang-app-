package com.catat.uang.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.catat.uang.data.model.Category

@Composable
fun CategoryManagerDialog(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onAddCategory: (String, String) -> Unit,
    onUpdateCategory: (Long, String, String) -> Unit,
    onDeleteCategory: (Long, (Boolean, String) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val palette = remember {
        listOf(
            "#D32F2F", "#F57C00", "#FBC02D", "#388E3C", "#00796B",
            "#0288D1", "#1565C0", "#5E35B1", "#7B1FA2", "#C2185B"
        )
    }

    var nameInput by remember { mutableStateOf("") }
    var selectedHex by remember { mutableStateOf(palette.first()) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kelola Kategori Kustom") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text(if (editingCategory == null) "Nama Kategori Baru" else "Ubah Nama Kategori") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Pilih Warna Grafik:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(palette) { hex ->
                        val isChosen = selectedHex.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(parseSafeColor(hex))
                                .then(
                                    if (isChosen) Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                )
                                .clickable { selectedHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isChosen) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val clean = nameInput.trim()
                            if (clean.isNotEmpty()) {
                                val target = editingCategory
                                if (target == null) {
                                    onAddCategory(clean, selectedHex)
                                    Toast.makeText(context, "Kategori '$clean' ditambahkan", Toast.LENGTH_SHORT).show()
                                } else {
                                    onUpdateCategory(target.id, clean, selectedHex)
                                    Toast.makeText(context, "Kategori diperbarui", Toast.LENGTH_SHORT).show()
                                    editingCategory = null
                                }
                                nameInput = ""
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (editingCategory == null) "Tambah Kategori" else "Simpan Perubahan")
                    }
                    if (editingCategory != null) {
                        OutlinedButton(onClick = { editingCategory = null; nameInput = "" }) {
                            Text("Batal")
                        }
                    }
                }

                HorizontalDivider()

                LazyColumn(
                    modifier = Modifier.heightIn(max = 220.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(categories) { cat ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(parseSafeColor(cat.colorHex))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    editingCategory = cat
                                    nameInput = cat.name
                                    selectedHex = cat.colorHex
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Kategori", modifier = Modifier.size(17.dp))
                            }
                            IconButton(
                                onClick = {
                                    onDeleteCategory(cat.id) { _, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus Kategori", tint = Color.Gray, modifier = Modifier.size(17.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Selesai") }
        }
    )
}

private fun parseSafeColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        Color(0xFF1565C0)
    }
}
