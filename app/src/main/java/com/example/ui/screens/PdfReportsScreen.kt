package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
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
                        text = "Generează document PDF imprimabil cu indecșii transmiși, consumul calculat și costul estimativ.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 2. Period Selection Chips
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Alege Perioada pentru Raport:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedPeriodType == 0,
                        onClick = { selectedPeriodType = 0 },
                        label = { Text("Luna Curentă") }
                    )
                    FilterChip(
                        selected = selectedPeriodType == 1,
                        onClick = { selectedPeriodType = 1 },
                        label = { Text("Ultimele 3 Luni") }
                    )
                    FilterChip(
                        selected = selectedPeriodType == 2,
                        onClick = { selectedPeriodType = 2 },
                        label = { Text("Tot Istoricul") }
                    )
                }
            }
        }

        // 3. Report Preview KPIs
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

            MetricCard(
                title = "Total General",
                value = String.format(Locale.US, "%.2f LEI", totalCost),
                subtitle = "${periodReadings.size} citiri",
                icon = Icons.Default.Paid,
                accentColor = CostGreen,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 4. Generate Button
        Button(
            onClick = { onExportPdf(periodLabel) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("generate_pdf_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )
        ) {
            Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Generează și Trimite Raportul PDF",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // 6. Share last generated PDF if available
        if (lastGeneratedFile != null && lastGeneratedFile.exists()) {
            OutlinedButton(
                onClick = { PdfReportGenerator.sharePdf(context, lastGeneratedFile) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Re-trimite ultimul raport generat (${lastGeneratedFile.name.take(24)}...)", fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
