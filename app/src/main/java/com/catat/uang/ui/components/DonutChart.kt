package com.catat.uang.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.catat.uang.data.model.CategorySummary
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.hypot

@Composable
fun DonutChart(
    summaryList: List<CategorySummary>,
    totalExpense: Long
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    // Reset pilihan saat bulan atau daftar kategori berubah
    LaunchedEffect(summaryList) {
        selectedIndex = null
    }

    val formatter = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 0
        }
    }

    if (summaryList.isEmpty() || totalExpense <= 0L) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Belum ada data pengeluaran pada periode ini.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
        return
    }

    val sweepAngles = remember(summaryList, totalExpense) {
        summaryList.map { (it.totalAmount.toFloat() / totalExpense.toFloat()) * 360f }
    }

    val selectedItem = selectedIndex?.let { summaryList.getOrNull(it) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(210.dp),
            contentAlignment = Alignment.Center
        ) {
            val animatedExtras = summaryList.indices.map { idx ->
                animateFloatAsState(
                    targetValue = if (selectedIndex == idx) 14f else 0f,
                    label = "slice_pop_$idx"
                ).value
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
                    // Deteksi sorotan kursor mouse / stylus
                    .pointerInput(sweepAngles) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: continue
                                if (change.type == PointerType.Mouse || change.type == PointerType.Stylus) {
                                    when (event.type) {
                                        PointerEventType.Move, PointerEventType.Enter -> {
                                            val hit = findSliceAtOffset(
                                                change.position,
                                                Size(size.width.toFloat(), size.height.toFloat()),
                                                sweepAngles
                                            )
                                            if (hit != null) selectedIndex = hit
                                        }
                                        PointerEventType.Exit -> {
                                            selectedIndex = null
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // Deteksi ketukan jari pada layar sentuh
                    .pointerInput(sweepAngles) {
                        detectTapGestures { tapOffset ->
                            val hit = findSliceAtOffset(
                                tapOffset,
                                Size(size.width.toFloat(), size.height.toFloat()),
                                sweepAngles
                            )
                            selectedIndex = if (hit == selectedIndex) null else hit
                        }
                    }
            ) {
                val baseStroke = size.minDimension * 0.19f
                var startAngle = -90f

                summaryList.forEachIndexed { index, item ->
                    val sweep = sweepAngles[index]
                    val isSelected = selectedIndex == index
                    val dimAlpha = if (selectedIndex != null && !isSelected) 0.4f else 1f
                    val sliceColor = parseHexColor(item.colorHex).copy(alpha = dimAlpha)
                    val strokeWidth = baseStroke + animatedExtras[index]
                    val gap = if (sweepAngles.size > 1) 2f else 0f

                    drawArc(
                        color = sliceColor,
                        startAngle = startAngle + (gap / 2f),
                        sweepAngle = (sweep - gap).coerceAtLeast(1f),
                        useCenter = false,
                        topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                        size = Size(size.width - strokeWidth, size.height - strokeWidth),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )
                    startAngle += sweep
                }
            }

            // Penjelasan detail tepat di tengah lubang donat
            Column(
                modifier = Modifier.width(114.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (selectedItem != null) {
                    val pct = (selectedItem.totalAmount.toDouble() / totalExpense.toDouble()) * 100.0
                    Text(
                        text = selectedItem.categoryName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatter.format(selectedItem.totalAmount),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = parseHexColor(selectedItem.colorHex),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = String.format(Locale("id", "ID"), "%.1f%%", pct),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                } else {
                    Text(
                        text = "Total Keluar",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    Text(
                        text = formatter.format(totalExpense),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Ketuk / sorot warna",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Legenda warna interaktif di bawah grafik
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            summaryList.forEachIndexed { index, item ->
                val isSelected = selectedIndex == index
                val pct = (item.totalAmount.toDouble() / totalExpense.toDouble()) * 100.0

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            selectedIndex = if (isSelected) null else index
                        },
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                    } else {
                        Color.Transparent
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(item.colorHex))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.categoryName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = String.format(Locale("id", "ID"), "%.0f%% • ", pct) + formatter.format(item.totalAmount),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else Color.Gray
                        )
                    }
                }
            }
        }
    }
}

private fun findSliceAtOffset(
    offset: Offset,
    canvasSize: Size,
    sweepAngles: List<Float>
): Int? {
    val cx = canvasSize.width / 2f
    val cy = canvasSize.height / 2f
    val dx = offset.x - cx
    val dy = offset.y - cy
    val distance = hypot(dx, dy)
    val outerRadius = minOf(cx, cy)
    val innerRadius = outerRadius * 0.48f

    if (distance < innerRadius || distance > outerRadius) return null

    val rawAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    val normalizedAngle = (rawAngle + 90f + 360f) % 360f

    var cumulative = 0f
    for (i in sweepAngles.indices) {
        val end = cumulative + sweepAngles[i]
        if (normalizedAngle >= cumulative && normalizedAngle <= end) {
            return i
        }
        cumulative = end
    }
    return null
}

private fun parseHexColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color(0xFF6750A4)
    }
}
