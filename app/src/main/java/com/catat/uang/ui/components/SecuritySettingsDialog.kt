package com.catat.uang.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.catat.uang.util.SecurityManager

@Composable
fun SecuritySettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var pinEnabled by remember { mutableStateOf(SecurityManager.isPinEnabled(context)) }
    var bioEnabled by remember { mutableStateOf(SecurityManager.isBiometricEnabled(context)) }
    val bioSupported = remember { SecurityManager.canUseBiometric(context) }
    var newPin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Keamanan Aplikasi") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (pinEnabled) "Status: Kunci PIN Aktif" else "Status: Tidak Terkunci",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) newPin = it },
                    label = { Text(if (pinEnabled) "Ganti PIN Baru (6 Digit)" else "Buat PIN Baru (6 Digit)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (bioSupported) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Buka dengan Sidik Jari / Biometrik", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        Switch(
                            checked = bioEnabled,
                            onCheckedChange = { checked ->
                                bioEnabled = checked
                                SecurityManager.setBiometricEnabled(context, checked)
                            }
                        )
                    }
                }
                if (pinEnabled) {
                    TextButton(
                        onClick = {
                            SecurityManager.disableSecurity(context)
                            pinEnabled = false
                            bioEnabled = false
                            Toast.makeText(context, "Kunci keamanan dinonaktifkan", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Matikan Kunci PIN")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newPin.length == 6) {
                        SecurityManager.savePin(context, newPin)
                        Toast.makeText(context, "PIN 6-digit tersimpan", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    } else if (newPin.isEmpty() && pinEnabled) {
                        onDismiss()
                    } else {
                        Toast.makeText(context, "PIN harus tepat 6 angka", Toast.LENGTH_SHORT).show()
                    }
                }
            ) { Text("Simpan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}
