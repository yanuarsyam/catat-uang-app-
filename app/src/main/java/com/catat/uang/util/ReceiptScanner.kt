package com.catat.uang.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.catat.uang.BuildConfig
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume
import kotlin.math.abs

data class ParsedReceipt(
    val merchant: String = "",
    val totalAmount: Long = 0L,
    val sourceEngine: String = "Offline OCR"
)

object ReceiptScanner {
    private const val PREFS_NAME = "catat_uang_prefs"
    private const val KEY_GEMINI_API = "gemini_api_key"
    private const val KEY_CUSTOM_SET = "gemini_custom_set"

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun saveApiKey(context: Context, apiKey: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_GEMINI_API, apiKey.trim())
            .putBoolean(KEY_CUSTOM_SET, true)
            .apply()
    }

    fun getApiKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return if (prefs.getBoolean(KEY_CUSTOM_SET, false)) {
            prefs.getString(KEY_GEMINI_API, "") ?: ""
        } else {
            BuildConfig.GEMINI_API_KEY
        }
    }

    suspend fun parseFromUri(context: Context, uri: Uri): ParsedReceipt {
        val apiKey = getApiKey(context)
        if (apiKey.isNotBlank()) {
            val aiResult = parseWithGemini(context, uri, apiKey)
            if (aiResult != null && aiResult.totalAmount > 0L) {
                return aiResult
            }
        }
        return parseWithSpatialMlKit(context, uri)
    }

    private suspend fun parseWithGemini(context: Context, uri: Uri, apiKey: String): ParsedReceipt? =
        withContext(Dispatchers.IO) {
            try {
                val base64Image = encodeImageToBase64(context, uri) ?: return@withContext null
                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    connectTimeout = 15000
                    readTimeout = 20000
                    doOutput = true
                }

                val prompt = """
                    Analisis gambar struk belanja atau catatan tulisan tangan ini.
                    Ekstrak nama toko/keterangan utama dan TOTAL akhir pembayaran bersih (abaikan nomor barcode/SKU/NPWP, perhitungkan diskon jika ada).
                    Balas HANYA dengan format JSON valid tanpa markdown:
                    {"merchant": "Nama Toko atau Ringkasan Catatan", "total": 313430}
                """.trimIndent()

                val payload = JSONObject().apply {
                    put("contents", JSONArray().put(
                        JSONObject().put("parts", JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                            put(JSONObject().put("inline_data", JSONObject().apply {
                                put("mime_type", "image/jpeg")
                                put("data", base64Image)
                            }))
                        })
                    ))
                }

                conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                if (conn.responseCode != 200) return@withContext null

                val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(responseStr)
                val rawText = root.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                    .replace("```json", "")
                    .replace("```", "")
                    .trim()

                val parsedJson = JSONObject(rawText)
                val merchant = parsedJson.optString("merchant", "Transaksi Struk")
                val total = parsedJson.optLong("total", 0L)
                ParsedReceipt(merchant = merchant, totalAmount = total, sourceEngine = "Gemini AI")
            } catch (e: Exception) {
                null
            }
        }

    private fun encodeImageToBase64(context: Context, uri: Uri): String? {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            val original = BitmapFactory.decodeStream(input)
            input.close()
            if (original == null) return null

            val maxDim = 1024
            val scale = minOf(maxDim.toFloat() / original.width, maxDim.toFloat() / original.height, 1f)
            val scaled = if (scale < 1f) {
                Bitmap.createScaledBitmap(original, (original.width * scale).toInt(), (original.height * scale).toInt(), true)
            } else original

            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 80, out)
            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun parseWithSpatialMlKit(context: Context, uri: Uri): ParsedReceipt =
        suspendCancellableCoroutine { continuation ->
            try {
                val image = InputImage.fromFilePath(context, uri)
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        continuation.resume(extractSpatialData(visionText))
                    }
                    .addOnFailureListener {
                        continuation.resume(ParsedReceipt())
                    }
            } catch (e: Exception) {
                continuation.resume(ParsedReceipt())
            }
        }

    private fun extractSpatialData(visionText: Text): ParsedReceipt {
        val allLines = visionText.textBlocks.flatMap { it.lines }
        if (allLines.isEmpty()) return ParsedReceipt()

        val sortedByY = allLines.sortedBy { it.boundingBox?.centerY() ?: 0 }
        val visualRows = mutableListOf<MutableList<Text.Line>>()

        for (line in sortedByY) {
            val box = line.boundingBox ?: continue
            val matchingRow = visualRows.find { row ->
                val refBox = row.first().boundingBox ?: return@find false
                val tolerance = maxOf(refBox.height(), box.height()) * 0.65
                abs(refBox.centerY() - box.centerY()) <= tolerance
            }
            if (matchingRow != null) {
                matchingRow.add(line)
            } else {
                visualRows.add(mutableListOf(line))
            }
        }

        val rowStrings = visualRows.map { row ->
            row.sortedBy { it.boundingBox?.left ?: 0 }.joinToString(" ") { it.text.trim() }
        }

        val merchant = rowStrings.take(4).firstOrNull { line ->
            line.length in 3..40 && line.count { it.isLetter() } > line.count { it.isDigit() }
        } ?: rowStrings.first()

        var detectedTotal = 0L
        val totalKeywords = listOf("grand total", "total :", "total:", "total ", "tunai", "debit", "bayar")

        for (row in rowStrings) {
            val lower = row.lowercase()
            if (lower.contains("subtotal") || lower.contains("sub total")) continue
            if (totalKeywords.any { lower.contains(it) }) {
                val amounts = extractCurrencyNumbers(row)
                if (amounts.isNotEmpty()) {
                    detectedTotal = amounts.last()
                    break
                }
            }
        }

        if (detectedTotal == 0L) {
            val candidates = rowStrings.flatMap { extractCurrencyNumbers(it) }
            detectedTotal = candidates.filter { it in 500..25_000_000 }.maxOrNull() ?: 0L
        }

        return ParsedReceipt(merchant = merchant, totalAmount = detectedTotal, sourceEngine = "Offline OCR")
    }

    private fun extractCurrencyNumbers(line: String): List<Long> {
        val formattedRegex = Regex("""\b\d{1,3}(?:[.,]\d{3})+\b""")
        val formattedMatches = formattedRegex.findAll(line).mapNotNull {
            it.value.replace(".", "").replace(",", "").toLongOrNull()
        }.toList()

        if (formattedMatches.isNotEmpty()) return formattedMatches

        val plainRegex = Regex("""(?<!\d)\d{3,7}(?!\d)""")
        return plainRegex.findAll(line).mapNotNull { it.value.toLongOrNull() }.toList()
    }
}
