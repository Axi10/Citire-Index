package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MeterReading
import com.example.data.model.UtilityType
import com.example.pdf.PdfReportGenerator
import com.example.ui.components.MetricCard
import com.example.ui.theme.CostGreen
import com.example.ui.theme.ElectricityAmber
import com.example.ui.theme.GasCyan
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PdfReportsScreen(
    readings: List<MeterReading>,
    lastGeneratedFile: File?,
    onExportPdf: (periodName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val currentMonthFormat = SimpleDateFormat("MMMM yyyy", Locale("ro", "RO"))
    val currentMonthLabel = remember { currentMonthFormat.format(Date()).replaceFirstChar { it.uppercase() } }

    var selectedPeriodType by remember { mutableStateOf(0) } // 0: Luna Curentă, 1: Ultimele 3 Luni, 2: Tot Istoricul

    val periodLabel = when (selectedPeriodType) {
        0 -> currentMonthLabel
        1 -> "Ultimele 3 Luni"
        else -> "Istoric Complet"
    }

    val periodReadings = remember(readings, selectedPeriodType) {
        val now = System.currentTimeMillis()
        when (selectedPeriodType) {
            0 -> {
                val oneMonthAgo = now - 31L * 24 * 3600 * 1000
                readings.filter { it.timestamp >= oneMonthAgo }
            }
            1 -> {
                val threeMonthsAgo = now - 92L * 24 * 3600 * 1000
                readings.filter { it.timestamp >= threeMonthsAgo }
            }
            else -> readings
        }
    }

    val totalGas = periodReadings.filter { it.utilityType == UtilityType.GAS }.sumOf { it.consumption }
    val totalGasCost = periodReadings.filter { it.utilityType == UtilityType.GAS }.sumOf { it.estimatedCost }
    val totalElec = periodReadings.filter { it.utilityType == UtilityType.ELECTRICITY }.sumOf { it.consumption }
    val totalElecCost = periodReadings.filter { it.utilityType == UtilityType.ELECTRICITY }.sumOf { it.estimatedCost }
    val totalCost = totalGasCost + totalElecCost

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Export Raport PDF Lunar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Document A4 imprimabil cu indecșii transmiși, consumul și costul estimativ.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 2. Period Selection: Custom Segmented Control (no overflow, perfectly balanced)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Alege Perioada pentru Raport:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(3.dp)
                    ) {
                        val options = listOf("Luna curentă", "Ultimele 3 luni", "Tot istoricul")
                        options.forEachIndexed { index, title ->
                            val isSelected = selectedPeriodType == index
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedPeriodType = index },
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = title,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Report Preview KPIs (2 side-by-side cards + 1 full-width featured summary banner)
        Text(
            text = "Sumar $periodLabel",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                title = "Consum Gaz",
                value = String.format(Locale.US, "%.0f m³", totalGas),
                subtitle = String.format(Locale.US, "%.2f LEI", totalGasCost),
                icon = Icons.Default.LocalFireDepartment,
                accentColor = GasCyan,
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "Consum Curent",
                value = String.format(Locale.US, "%.0f kWh", totalElec),
                subtitle = String.format(Locale.US, "%.2f LEI", totalElecCost),
                icon = Icons.Default.ElectricBolt,
                accentColor = ElectricityAmber,
                modifier = Modifier.weight(1f)
            )
        }

        // Prominent Total General Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CostGreen.copy(alpha = 0.08f)),
            border = BorderStroke(1.dp, CostGreen.copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(CostGreen.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Paid,
                            contentDescription = null,
                            tint = CostGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Total General Estimativ",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${periodReadings.size} citiri înregistrate în perioadă",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = String.format(Locale.US, "%.2f LEI", totalCost),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CostGreen
                )
            }
        }

        // 4. Live Preview of Readings included in the PDF
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Citiri incluse în raport (${periodReadings.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (periodReadings.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nu există citiri pentru perioada selectată.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val previewFormat = SimpleDateFormat("dd MMM yyyy", Locale("ro", "RO"))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        periodReadings.take(5).forEach { item ->
                            val isGas = item.utilityType == UtilityType.GAS
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isGas) Icons.Default.LocalFireDepartment else Icons.Default.ElectricBolt,
                                        contentDescription = null,
                                        tint = if (isGas) GasCyan else ElectricityAmber,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${if (isGas) "Gaz" else "Curent"} • ${previewFormat.format(Date(item.timestamp))}",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    text = "${String.format(Locale.US, "%.0f", item.indexValue)} ${item.utilityType.unit} (${String.format(Locale.US, "%.2f", item.estimatedCost)} lei)",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        if (periodReadings.size > 5) {
                            Text(
                                text = "+ încă ${periodReadings.size - 5} citiri vor fi incluse în fișierul PDF complet",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // 5. Generate Button
        Button(
            onClick = { onExportPdf(periodLabel) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("generate_pdf_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )
        ) {
            Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(19.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Generează și Trimite Raportul PDF",
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // 6. Share last generated PDF if available
        if (lastGeneratedFile != null && lastGeneratedFile.exists()) {
            OutlinedButton(
                onClick = { PdfReportGenerator.sharePdf(context, lastGeneratedFile) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Re-trimite ultimul raport (${lastGeneratedFile.name.take(22)}...)", fontSize = 12.5.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}
