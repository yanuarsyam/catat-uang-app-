package com.catat.uang.util

import android.content.Context
import android.net.Uri
import com.catat.uang.data.model.TransactionItem
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
            val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale("id", "ID"))
            val timeFmt = SimpleDateFormat("HH:mm", Locale("id", "ID"))

            val sb = StringBuilder()
            sb.appendLine("LAPORAN CATATAN KEUANGAN ($periodLabel)")
            sb.appendLine("Tanggal,Jam,Jenis,Kategori,Dompet/Rekening,Nominal,Keterangan")

            var totalIn = 0L
            var totalOut = 0L

            for (item in transactions) {
                val d = Date(item.timestamp)
                val jenis = if (item.type == "INCOME") "Pemasukan" else "Pengeluaran"
                if (item.type == "INCOME") totalIn += item.amount else totalOut += item.amount

                val row = listOf(
                    dateFmt.format(d),
                    timeFmt.format(d),
                    jenis,
                    escapeCsv(item.categoryName),
                    escapeCsv(item.accountName),
                    item.amount.toString(),
                    escapeCsv(item.note)
                ).joinToString(",")
                sb.appendLine(row)
            }

            sb.appendLine()
            sb.appendLine("Total Pemasukan,,,,,${totalIn},")
            sb.appendLine("Total Pengeluaran,,,,,${totalOut},")
            sb.appendLine("Selisih Bersih,,,,,${totalIn - totalOut},")

            context.contentResolver.openOutputStream(uri)?.use { out ->
                // UTF-8 BOM agar Excel langsung membaca karakter dan kolom dengan rapi
                out.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                out.write(sb.toString().toByteArray(Charsets.UTF_8))
                out.flush()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun escapeCsv(text: String): String {
        val clean = text.replace("\"", "\"\"").replace("\n", " ")
        return "\"$clean\""
    }
}
