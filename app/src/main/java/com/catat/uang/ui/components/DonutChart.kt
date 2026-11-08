package com.catat.uang.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.catat.uang.data.model.CategorySummary
import java.text.NumberFormat
import java.util.Locale

@Composable
fun DonutChart(
    summaryList: List<CategorySummary>,
    totalExpense: Long,
    modifier: Modifier = Modifier
) {
    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }

    Box(
        modifier = modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val strokeWidth = 32.dp.toPx()
            if (summaryList.isEmpty() || totalExpense == 0L) {
                drawCircle(
                    color = Color.LightGray.copy(alpha = 0.4f),
                    style = Stroke(width = strokeWidth)
                )
            } else {
                var startAngle = -90f
                summaryList.forEach { item ->
                    val sweep = (item.totalAmount.toFloat() / totalExpense.toFloat()) * 360f
                    val color = runCatching { Color(android.graphics.Color.parseColor(item.colorHex)) }
                        .getOrDefault(Color.Gray)

                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )
                    startAngle += sweep
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Pengeluaran",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
            Text(
                text = formatter.format(totalExpense),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
