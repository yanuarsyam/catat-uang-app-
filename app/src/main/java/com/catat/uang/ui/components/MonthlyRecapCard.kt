package com.catat.uang.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
    onExportCsvClick: () -> Unit
) {
    val label = if (selectedMonth == null) {
        "Semua Waktu"
    } else {
        selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("id", "ID")))
            .replaceFirstChar { it.uppercase() }
    }
    val netFlow = monthlyIncome - monthlyExpense

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPrevMonth, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Bulan Sebelumnya")
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    IconButton(onClick = onNextMonth, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Bulan Berikutnya")
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = selectedMonth == null,
                        onClick = onToggleAllTime,
                        label = { Text("Semua", style = MaterialTheme.typography.labelSmall) }
                    )
                    FilledTonalIconButton(onClick = onExportCsvClick, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Ekspor CSV", modifier = Modifier.size(18.dp))
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Pemasukan", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(formatter.format(monthlyIncome), style = MaterialTheme.typography.bodyMedium, color = Color(0xFF2E7D32), fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text("Pengeluaran", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(formatter.format(monthlyExpense), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Selisih", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(
                        text = formatter.format(netFlow),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (netFlow >= 0) Color(0xFF1565C0) else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
