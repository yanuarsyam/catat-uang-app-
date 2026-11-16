package com.catat.uang.util

import android.content.Context
import android.net.Uri
import com.catat.uang.data.model.TransactionItem
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

object CsvExporter {
    fun writeCsvToUri(
        context: Context,
        uri: Uri,
        transactions: List<TransactionItem>,
        periodLabel: String
    ): Boolean {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale("id", "ID"))
            context.contentResolver.openOutputStream(uri)?.use { out ->
                OutputStreamWriter(out, Charsets.UTF_8).use { writer ->
                    writer.append("Tanggal,Tipe,Dompet / Akun,Kategori,Nominal (Rp),Keterangan\n")
                    transactions.forEach { item ->
                        val dateStr = sdf.format(Date(item.timestamp))
                        val typeLabel = when (item.type) {
                            "INCOME" -> "Pemasukan"
                            "TRANSFER" -> "Transfer"
                            else -> "Pengeluaran"
                        }
                        val accountLabel = if (item.type == "TRANSFER") {
                            "${item.accountName} -> ${item.targetAccountName ?: ""}"
                        } else {
                            item.accountName
                        }
                        val categoryLabel = if (item.type == "TRANSFER") "Transfer Saldo" else item.categoryName
                        val safeNote = "\"${item.note.replace("\"", "\"\"")}\""
                        writer.append("$dateStr,$typeLabel,\"$accountLabel\",\"$categoryLabel\",${item.amount},$safeNote\n")
                    }
                    writer.flush()
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
