package com.catat.uang.util

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class ParsedReceipt(
    val merchant: String = "",
    val totalAmount: Long = 0L
)

object ReceiptScanner {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun parseFromUri(context: Context, uri: Uri): ParsedReceipt =
        suspendCancellableCoroutine { continuation ->
            try {
                val image = InputImage.fromFilePath(context, uri)
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        continuation.resume(extractData(visionText.text))
                    }
                    .addOnFailureListener {
                        continuation.resume(ParsedReceipt())
                    }
            } catch (e: Exception) {
                continuation.resume(ParsedReceipt())
            }
        }

    private fun extractData(text: String): ParsedReceipt {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return ParsedReceipt()

        val merchant = lines.take(4).firstOrNull { line ->
            line.length in 3..35 && line.count { it.isLetter() } > line.count { it.isDigit() }
        } ?: lines.first()

        val keywords = listOf("total", "grand total", "tunai", "cash", "bayar", "rp", "netto")
        var detectedTotal = 0L

        for (i in lines.indices) {
            val lower = lines[i].lowercase()
            if (keywords.any { lower.contains(it) }) {
                val numbers = extractNumbers(lines[i])
                if (numbers.isNotEmpty()) {
                    detectedTotal = numbers.maxOrNull() ?: 0L
                    break
                } else if (i + 1 < lines.size) {
                    val nextNums = extractNumbers(lines[i + 1])
                    if (nextNums.isNotEmpty()) {
                        detectedTotal = nextNums.maxOrNull() ?: 0L
                        break
                    }
                }
            }
        }

        if (detectedTotal == 0L) {
            val allNumbers = lines.flatMap { extractNumbers(it) }
            detectedTotal = allNumbers.filter { it in 1000..50_000_000 }.maxOrNull() ?: 0L
        }

        return ParsedReceipt(merchant = merchant, totalAmount = detectedTotal)
    }

    private fun extractNumbers(line: String): List<Long> {
        val cleaned = line.replace(".", "").replace(",", "").replace("Rp", "", ignoreCase = true)
        val regex = Regex("""\b\d{3,9}\b""")
        return regex.findAll(cleaned).mapNotNull { it.value.toLongOrNull() }.toList()
    }
}
