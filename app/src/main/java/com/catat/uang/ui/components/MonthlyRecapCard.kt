package com.catat.uang.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MonthlyRecapCard(
    selectedMonth: YearMonth?,
    monthlyIncome: Long,
    monthlyExpense: Long,
    formatter: NumberFormat,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToggleAllTime: () -> Unit,
    onExportCsvClick: () -> Unit,
    onImportCsvClick: () -> Unit
) {
    val monthFormatter = remember {
        DateTimeFormatter.ofPattern("MMMM yyyy", Locale("id", "ID"))
    }
    val periodTitle = selectedMonth?.format(monthFormatter) ?: "Semua Waktu"
    val netDiff = monthlyIncome - monthlyExpense

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPrevMonth, enabled = selectedMonth != null) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Bulan Sebelumnya")
                    }
                    Text(
                        text = periodTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onNextMonth, enabled = selectedMonth != null) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Bulan Berikutnya")
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = selectedMonth == null,
                        onClick = onToggleAllTime,
                        label = { Text("Semua", style = MaterialTheme.typography.labelSmall) }
                    )
                    IconButton(onClick = onImportCsvClick) {
                        Icon(Icons.Default.UploadFile, contentDescription = "Impor / Restore CSV")
                    }
                    IconButton(onClick = onExportCsvClick) {
                        Icon(Icons.Default.Download, contentDescription = "Ekspor CSV")
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Pemasukan", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(
                        text = formatter.format(monthlyIncome),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Pengeluaran", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(
                        text = formatter.format(monthlyExpense),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Selisih", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(
                        text = formatter.format(netDiff),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (netDiff >= 0) Color(0xFF1565C0) else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
