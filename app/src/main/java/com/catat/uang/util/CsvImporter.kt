package com.catat.uang.util

import android.content.Context
import android.net.Uri
import com.catat.uang.data.local.FinanceDao
import com.catat.uang.data.model.Account
import com.catat.uang.data.model.Category
import com.catat.uang.data.model.TransactionEntity
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

object CsvImporter {
    private val palette = listOf("#D32F2F", "#F57C00", "#0288D1", "#7B1FA2", "#388E3C", "#00796B", "#5E35B1")

    suspend fun importCsvFromUri(context: Context, uri: Uri, dao: FinanceDao): Int {
        var importedCount = 0
        val sdfFull = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale("id", "ID"))
        val sdfDateOnly = SimpleDateFormat("yyyy-MM-dd", Locale("id", "ID"))

        val accountMap = dao.getAllAccountsList().associateBy { it.name.lowercase() }.toMutableMap()
        val categoryMap = dao.getAllCategoriesList().associateBy { it.name.lowercase() }.toMutableMap()

        suspend fun getOrCreateAccount(rawName: String): Account {
            val clean = rawName.trim().ifBlank { "Dompet Tunai" }
            val key = clean.lowercase()
            accountMap[key]?.let { return it }
            val newId = dao.insertAccount(Account(name = clean, balance = 0L, colorHex = "#1565C0"))
            val created = Account(id = newId, name = clean, balance = 0L, colorHex = "#1565C0")
            accountMap[key] = created
            return created
        }

        suspend fun getOrCreateCategory(rawName: String): Category {
            val clean = rawName.trim().ifBlank { "Lainnya" }
            val key = clean.lowercase()
            categoryMap[key]?.let { return it }
            val color = palette[categoryMap.size % palette.size]
            val newId = dao.insertCategory(Category(name = clean, colorHex = color))
            val created = Category(id = newId, name = clean, colorHex = color)
            categoryMap[key] = created
            return created
        }

        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                    var lineIndex = 0
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val current = line!!.trim()
                        lineIndex++
                        if (current.isEmpty()) continue
                        if (lineIndex == 1 && current.lowercase().startsWith("tanggal")) continue

                        val cols = parseCsvLine(current)
                        if (cols.size < 5) continue

                        val timestamp = try {
                            sdfFull.parse(cols[0].trim())?.time
                                ?: sdfDateOnly.parse(cols[0].trim())?.time
                                ?: System.currentTimeMillis()
                        } catch (_: Exception) {
                            System.currentTimeMillis()
                        }

                        val rawType = cols[1].trim().lowercase()
                        val rawAccount = cols[2].trim()
                        val rawCategory = cols[3].trim()
                        val amount = cols[4].replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0L
                        val note = if (cols.size >= 6) cols[5].trim() else ""

                        if (amount <= 0L) continue

                        if (rawType.contains("transfer") && rawAccount.contains("->")) {
                            val parts = rawAccount.split("->")
                            val fromAcc = getOrCreateAccount(parts[0])
                            val toAcc = getOrCreateAccount(parts.getOrElse(1) { "Dompet Tujuan" })
                            val defaultCat = categoryMap.values.firstOrNull() ?: getOrCreateCategory("Lainnya")
                            dao.executeTransfer(
                                fromAccountId = fromAcc.id,
                                toAccountId = toAcc.id,
                                categoryId = defaultCat.id,
                                amount = amount,
                                timestamp = timestamp,
                                note = note
                            )
                            importedCount++
                        } else {
                            val type = if (rawType.contains("masuk") || rawType == "income") "INCOME" else "EXPENSE"
                            val acc = getOrCreateAccount(rawAccount)
                            val cat = getOrCreateCategory(rawCategory)
                            dao.insertTransaction(
                                TransactionEntity(
                                    accountId = acc.id,
                                    categoryId = cat.id,
                                    amount = amount,
                                    type = type,
                                    timestamp = timestamp,
                                    note = note
                                )
                            )
                            val delta = if (type == "INCOME") amount else -amount
                            dao.updateAccountBalance(acc.id, delta)
                            importedCount++
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
        return importedCount
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    sb.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                result.add(sb.toString())
                sb.setLength(0)
            } else {
                sb.append(c)
            }
            i++
        }
        result.add(sb.toString())
        return result
    }
}
