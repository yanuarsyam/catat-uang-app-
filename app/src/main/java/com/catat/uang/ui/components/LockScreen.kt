package com.catat.uang.ui.components

import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.catat.uang.util.SecurityManager

@Composable
fun LockScreen(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    var enteredPin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    val bioEnabled = remember { SecurityManager.isBiometricEnabled(context) && SecurityManager.canUseBiometric(context) }

    fun triggerBiometric() {
        val activity = context as? FragmentActivity ?: return
        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onUnlocked()
            }
        })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Buka Catatan Keuangan")
            .setSubtitle("Gunakan sidik jari atau biometrik perangkat")
            .setNegativeButtonText("Gunakan PIN")
            .build()
        prompt.authenticate(info)
    }

    LaunchedEffect(Unit) {
        if (bioEnabled) triggerBiometric()
    }

    fun onDigitClick(digit: String) {
        if (enteredPin.length < 6) {
            errorMsg = ""
            val updated = enteredPin + digit
            enteredPin = updated
            if (updated.length == 6) {
                if (SecurityManager.verifyPin(context, updated)) {
                    onUnlocked()
                } else {
                    errorMsg = "PIN salah, coba lagi"
                    enteredPin = ""
                }
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text("Catatan Keuangan Terkunci", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text("Masukkan 6 digit PIN Anda", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)

            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(6) { index ->
                    val filled = index < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (filled) MaterialTheme.colorScheme.primary else Color.LightGray)
                    )
                }
            }

            if (errorMsg.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(errorMsg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(28.dp))
            val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"))
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                    row.forEach { num ->
                        FilledTonalButton(
                            onClick = { onDigitClick(num) },
                            modifier = Modifier.size(68.dp),
                            shape = CircleShape
                        ) {
                            Text(num, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                if (bioEnabled) {
                    OutlinedButton(
                        onClick = { triggerBiometric() },
                        modifier = Modifier.size(68.dp),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = "Biometrik", modifier = Modifier.size(28.dp))
                    }
                } else {
                    Spacer(modifier = Modifier.size(68.dp))
                }

                FilledTonalButton(
                    onClick = { onDigitClick("0") },
                    modifier = Modifier.size(68.dp),
                    shape = CircleShape
                ) {
                    Text("0", style = MaterialTheme.typography.titleLarge)
                }

                OutlinedButton(
                    onClick = { if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1) },
                    modifier = Modifier.size(68.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Backspace, contentDescription = "Hapus")
                }
            }
        }
    }
}
