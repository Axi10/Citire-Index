package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MeterReading
import com.example.data.model.UtilityType
import com.example.ui.theme.ElectricityAmber
import com.example.ui.theme.GasCyan
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MonthlyBarData(
    val monthLabel: String,
    val gasConsumption: Double,
    val electricityConsumption: Double
)

@Composable
fun ConsumptionChart(
    readings: List<MeterReading>,
    selectedType: UtilityType?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Evoluție Consum Lunar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Legends
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (selectedType == null || selectedType == UtilityType.GAS) {
                        ChartLegendItem(color = GasCyan, label = "Gaz (m³)")
                    }
                    if (selectedType == null || selectedType == UtilityType.ELECTRICITY) {
                        ChartLegendItem(color = ElectricityAmber, label = "Curent (kWh)")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Build monthly aggregated data
            val monthlyData = prepareMonthlyData(readings)

            if (monthlyData.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nu există suficiente date pentru grafic",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val bottomPadding = 30.dp.toPx()
                    val topPadding = 32.dp.toPx()
                    val chartHeight = height - bottomPadding - topPadding

                    val maxGas = (monthlyData.maxOfOrNull { it.gasConsumption } ?: 10.0).coerceAtLeast(6.0) * 1.2
                    val maxElec = (monthlyData.maxOfOrNull { it.electricityConsumption } ?: 50.0).coerceAtLeast(30.0) * 1.2

                    // Reference Grid lines
                    val gridPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.LTGRAY
                        alpha = 50
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = 1.dp.toPx()
                        pathEffect = android.graphics.DashPathEffect(floatArrayOf(8f, 8f), 0f)
                    }

                    // 50% line
                    val midY = topPadding + chartHeight / 2
                    drawContext.canvas.nativeCanvas.drawLine(0f, midY, width, midY, gridPaint)

                    val slotWidth = width / monthlyData.size.toFloat()
                    val singleBarWidth = 20.dp.toPx()
                    val dualBarWidth = 12.dp.toPx()

                    val textPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.rgb(100, 116, 139)
                        textSize = 11.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                        isFakeBoldText = true
                    }

                    val valuePaint = android.graphics.Paint().apply {
                        textSize = 10.5.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                        isFakeBoldText = true
                    }

                    val trackColor = Color(0xFFF1F5F9)

                    monthlyData.forEachIndexed { index, data ->
                        val centerX = slotWidth * index + slotWidth / 2f

                        // Draw Month Label
                        drawContext.canvas.nativeCanvas.drawText(
                            data.monthLabel,
                            centerX,
                            height - 8.dp.toPx(),
                            textPaint
                        )

                        val showGas = (selectedType == null || selectedType == UtilityType.GAS)
                        val showElec = (selectedType == null || selectedType == UtilityType.ELECTRICITY)

                        if (selectedType == null && showGas && showElec) {
                            // Two bars side by side
                            val gasBarHeight = ((data.gasConsumption / maxGas) * chartHeight).toFloat().coerceIn(4f, chartHeight)
                            val elecBarHeight = ((data.electricityConsumption / maxElec) * chartHeight).toFloat().coerceIn(4f, chartHeight)

                            // Background tracks
                            val gasLeft = centerX - dualBarWidth - 2.dp.toPx()
                            val elecLeft = centerX + 2.dp.toPx()
                            drawRoundRect(
                                color = trackColor,
                                topLeft = Offset(gasLeft, topPadding),
                                size = Size(dualBarWidth, chartHeight),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                            drawRoundRect(
                                color = trackColor,
                                topLeft = Offset(elecLeft, topPadding),
                                size = Size(dualBarWidth, chartHeight),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )

                            // Gas bar
                            val gasTop = topPadding + chartHeight - gasBarHeight
                            drawRoundRect(
                                color = GasCyan,
                                topLeft = Offset(gasLeft, gasTop),
                                size = Size(dualBarWidth, gasBarHeight),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                            if (data.gasConsumption > 0) {
                                valuePaint.color = GasCyan.toArgb()
                                drawContext.canvas.nativeCanvas.drawText(
                                    String.format(Locale.US, "%.0f", data.gasConsumption),
                                    gasLeft + dualBarWidth / 2,
                                    (gasTop - 5.dp.toPx()).coerceAtLeast(topPadding - 2.dp.toPx()),
                                    valuePaint
                                )
                            }

                            // Electricity bar
                            val elecTop = topPadding + chartHeight - elecBarHeight
                            drawRoundRect(
                                color = ElectricityAmber,
                                topLeft = Offset(elecLeft, elecTop),
                                size = Size(dualBarWidth, elecBarHeight),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                            if (data.electricityConsumption > 0) {
                                valuePaint.color = ElectricityAmber.toArgb()
                                drawContext.canvas.nativeCanvas.drawText(
                                    String.format(Locale.US, "%.0f", data.electricityConsumption),
                                    elecLeft + dualBarWidth / 2,
                                    (elecTop - 5.dp.toPx()).coerceAtLeast(topPadding - 2.dp.toPx()),
                                    valuePaint
                                )
                            }
                        } else if (selectedType == UtilityType.GAS) {
                            val gasBarHeight = if (data.gasConsumption > 0) {
                                ((data.gasConsumption / maxGas) * chartHeight).toFloat().coerceIn(6f, chartHeight)
                            } else 0f
                            val gasLeft = centerX - singleBarWidth / 2

                            // Background Track
                            drawRoundRect(
                                color = trackColor,
                                topLeft = Offset(gasLeft, topPadding),
                                size = Size(singleBarWidth, chartHeight),
                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                            )

                            if (gasBarHeight > 0) {
                                val gasTop = topPadding + chartHeight - gasBarHeight
                                drawRoundRect(
                                    color = GasCyan,
                                    topLeft = Offset(gasLeft, gasTop),
                                    size = Size(singleBarWidth, gasBarHeight),
                                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                                )
                                valuePaint.color = android.graphics.Color.rgb(2, 132, 199)
                                drawContext.canvas.nativeCanvas.drawText(
                                    String.format(Locale.US, "%.0f m³", data.gasConsumption),
                                    centerX,
                                    (gasTop - 6.dp.toPx()).coerceAtLeast(topPadding - 4.dp.toPx()),
                                    valuePaint
                                )
                            } else {
                                valuePaint.color = android.graphics.Color.LTGRAY
                                drawContext.canvas.nativeCanvas.drawText(
                                    "-",
                                    centerX,
                                    topPadding + chartHeight - 8.dp.toPx(),
                                    valuePaint
                                )
                            }
                        } else {
                            // Electricity single bar
                            val elecBarHeight = if (data.electricityConsumption > 0) {
                                ((data.electricityConsumption / maxElec) * chartHeight).toFloat().coerceIn(6f, chartHeight)
                            } else 0f
                            val elecLeft = centerX - singleBarWidth / 2

                            // Background Track
                            drawRoundRect(
                                color = trackColor,
                                topLeft = Offset(elecLeft, topPadding),
                                size = Size(singleBarWidth, chartHeight),
                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                            )

                            if (elecBarHeight > 0) {
                                val elecTop = topPadding + chartHeight - elecBarHeight
                                drawRoundRect(
                                    color = ElectricityAmber,
                                    topLeft = Offset(elecLeft, elecTop),
                                    size = Size(singleBarWidth, elecBarHeight),
                                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                                )
                                valuePaint.color = android.graphics.Color.rgb(217, 119, 6)
                                drawContext.canvas.nativeCanvas.drawText(
                                    String.format(Locale.US, "%.0f kWh", data.electricityConsumption),
                                    centerX,
                                    (elecTop - 6.dp.toPx()).coerceAtLeast(topPadding - 4.dp.toPx()),
                                    valuePaint
                                )
                            } else {
                                valuePaint.color = android.graphics.Color.LTGRAY
                                drawContext.canvas.nativeCanvas.drawText(
                                    "-",
                                    centerX,
                                    topPadding + chartHeight - 8.dp.toPx(),
                                    valuePaint
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun prepareMonthlyData(readings: List<MeterReading>): List<MonthlyBarData> {
    val monthFormat = SimpleDateFormat("MMM", Locale("ro", "RO"))
    val keyFormat = SimpleDateFormat("yyyy-MM", Locale.US)

    // Group readings by month
    val grouped = readings.groupBy { keyFormat.format(Date(it.timestamp)) }
        .toSortedMap()

    // Take last 6 months
    val recentMonths = grouped.entries.toList().takeLast(6)

    return recentMonths.map { entry ->
        val list = entry.value
        val sampleDate = list.firstOrNull()?.timestamp ?: System.currentTimeMillis()
        val label = monthFormat.format(Date(sampleDate)).replaceFirstChar { it.uppercase() }
        val gasSum = list.filter { it.utilityType == UtilityType.GAS }.sumOf { it.consumption }
        val elecSum = list.filter { it.utilityType == UtilityType.ELECTRICITY }.sumOf { it.consumption }

        MonthlyBarData(
            monthLabel = label,
            gasConsumption = gasSum,
            electricityConsumption = elecSum
        )
    }
}
