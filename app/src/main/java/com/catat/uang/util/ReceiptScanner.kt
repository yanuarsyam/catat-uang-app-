package com.catat.uang.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.catat.uang.BuildConfig
import com.google.mlkit.vision.common.InputImage
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

data class ParsedReceiptItem(
    val name: String,
    val amount: Long,
    val suggestedCategory: String
)

data class ParsedReceipt(
    val totalAmount: Long,
    val merchant: String,
    val sourceEngine: String,
    val items: List<ParsedReceiptItem> = emptyList()
)

object ReceiptScanner {
    private const val PREF_NAME = "catat_uang_prefs"
    private const val KEY_GEMINI = "gemini_api_key"

    fun getApiKey(context: Context): String {
        val saved = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_GEMINI, "")?.trim().orEmpty()
        return saved.ifEmpty { BuildConfig.GEMINI_API_KEY.trim() }
    }

    fun saveApiKey(context: Context, key: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_GEMINI, key.trim()).apply()
    }

    suspend fun parseFromUri(
        context: Context,
        uri: Uri,
        availableCategories: List<String> = emptyList()
    ): ParsedReceipt = withContext(Dispatchers.IO) {
        val apiKey = getApiKey(context)
        if (apiKey.isNotEmpty()) {
            val geminiResult = parseWithGemini(context, uri, apiKey, availableCategories)
            if (geminiResult != null && geminiResult.totalAmount > 0L) {
                return@withContext geminiResult
            }
        }
        parseWithMlKit(context, uri)
    }

    private fun parseWithGemini(
        context: Context,
        uri: Uri,
        apiKey: String,
        availableCategories: List<String>
    ): ParsedReceipt? {
        return try {
            val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: return null

            val maxDim = 1280
            val scale = minOf(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height, 1f)
            val resized = if (scale < 1f) {
                Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
            } else bitmap

            val baos = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            val base64Image = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)

            val catHint = if (availableCategories.isNotEmpty()) {
                "Pilih 'category' paling cocok dari daftar ini: ${availableCategories.joinToString(", ")}."
            } else {
                "Isi 'category' dengan kategori pengeluaran umum bahasa Indonesia."
            }

            val promptText = """
                Ekstrak data struk belanja atau catatan pengeluaran ini menjadi JSON murni tanpa markdown:
                {
                  "merchant": "Nama toko atau keterangan utama",
                  "total": 0,
                  "items": [
                    {"name": "Nama barang/item", "amount": 0, "category": "Nama kategori"}
                  ]
                }
                Aturan: 'total' dan 'amount' berupa angka integer Rupiah tanpa titik/koma. $catHint
                Jika ada beberapa barang belanjaan, rinci ke dalam array 'items'.
            """.trimIndent()

            val payload = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", promptText))
                        put(JSONObject().put("inline_data", JSONObject().apply {
                            put("mime_type", "image/jpeg")
                            put("data", base64Image)
                        }))
                    })
                }))
            }

            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 20000
                readTimeout = 25000
                doOutput = true
            }
            conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }

            if (conn.responseCode != 200) return null
            val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
            val textResp = JSONObject(responseStr)
                .getJSONArray("candidates").getJSONObject(0)
                .getJSONObject("content").getJSONArray("parts").getJSONObject(0)
                .getString("text")

            val cleanJson = textResp.replace("```json", "").replace("```", "").trim()
            val obj = JSONObject(cleanJson)
            val merchant = obj.optString("merchant", "Belanja Struk").trim()
            var total = obj.optLong("total", 0L)

            val parsedItems = mutableListOf<ParsedReceiptItem>()
            val itemsArr = obj.optJSONArray("items")
            if (itemsArr != null) {
                for (i in 0 until itemsArr.length()) {
                    val itemObj = itemsArr.optJSONObject(i) ?: continue
                    val itemName = itemObj.optString("name", "").trim()
                    val itemAmt = itemObj.optLong("amount", 0L)
                    val itemCat = itemObj.optString("category", "").trim()
                    if (itemName.isNotEmpty() && itemAmt > 0L) {
                        parsedItems.add(ParsedReceiptItem(itemName, itemAmt, itemCat))
                    }
                }
            }
            if (total <= 0L && parsedItems.isNotEmpty()) {
                total = parsedItems.sumOf { it.amount }
            }

            ParsedReceipt(
                totalAmount = total,
                merchant = merchant.ifEmpty { "Belanja Struk" },
                sourceEngine = "Gemini AI",
                items = parsedItems
            )
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun parseWithMlKit(context: Context, uri: Uri): ParsedReceipt {
        return try {
            val image = InputImage.fromFilePath(context, uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val rawText = suspendCancellableCoroutine { cont ->
                recognizer.process(image)
                    .addOnSuccessListener { cont.resume(it.text) }
                    .addOnFailureListener { cont.resume("") }
            }
            val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
            val merchant = lines.firstOrNull { it.length > 2 && !it.all { c -> c.isDigit() } } ?: "Struk Offline"
            val numbers = Regex("""\b\d{1,3}(?:[.,]\d{3})+\b|\b\d{4,9}\b""")
                .findAll(rawText)
                .mapNotNull { it.value.replace(".", "").replace(",", "").toLongOrNull() }
                .filter { it in 500L..500_000_000L }
                .toList()
            ParsedReceipt(
                totalAmount = numbers.maxOrNull() ?: 0L,
                merchant = merchant,
                sourceEngine = "OCR Offline"
            )
        } catch (_: Exception) {
            ParsedReceipt(0L, "Struk", "OCR Offline")
        }
    }
}
