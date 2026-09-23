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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MeterReading
import com.example.data.model.UtilityType
import com.example.ui.components.ConsumptionChart
import com.example.ui.components.MetricCard
import com.example.ui.theme.CostGreen
import com.example.ui.theme.ElectricityAmber
import com.example.ui.theme.GasCyan
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    readings: List<MeterReading>,
    onDeleteReading: (MeterReading) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf<UtilityType?>(null) }

    val filteredReadings = remember(readings, selectedFilter) {
        if (selectedFilter == null) readings else readings.filter { it.utilityType == selectedFilter }
    }

    val totalGasCost = remember(readings) {
        readings.filter { it.utilityType == UtilityType.GAS }.sumOf { it.estimatedCost }
    }
    val totalElecCost = remember(readings) {
        readings.filter { it.utilityType == UtilityType.ELECTRICITY }.sumOf { it.estimatedCost }
    }
    val totalCost = totalGasCost + totalElecCost

    val totalGasCons = remember(readings) {
        readings.filter { it.utilityType == UtilityType.GAS }.sumOf { it.consumption }
    }
    val totalElecCons = remember(readings) {
        readings.filter { it.utilityType == UtilityType.ELECTRICITY }.sumOf { it.consumption }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Filter Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { selectedFilter = null },
                    label = { Text("Toate (${readings.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                FilterChip(
                    selected = selectedFilter == UtilityType.GAS,
                    onClick = { selectedFilter = UtilityType.GAS },
                    label = { Text("Gaz") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = GasCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )

                FilterChip(
                    selected = selectedFilter == UtilityType.ELECTRICITY,
                    onClick = { selectedFilter = UtilityType.ELECTRICITY },
                    label = { Text("Curent") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.ElectricBolt,
                            contentDescription = null,
                            tint = ElectricityAmber,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }

        // 2. High Level KPI Metrics
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Total Gaz",
                    value = String.format(Locale.US, "%.0f m³", totalGasCons),
                    subtitle = String.format(Locale.US, "%.2f LEI", totalGasCost),
                    icon = Icons.Default.LocalFireDepartment,
                    accentColor = GasCyan,
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "Total Curent",
                    value = String.format(Locale.US, "%.0f kWh", totalElecCons),
                    subtitle = String.format(Locale.US, "%.2f LEI", totalElecCost),
                    icon = Icons.Default.ElectricBolt,
                    accentColor = ElectricityAmber,
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "Total Cost",
                    value = String.format(Locale.US, "%.2f L", totalCost),
                    subtitle = "Estimare plată",
                    icon = Icons.Default.Paid,
                    accentColor = CostGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. Monthly Consumption Chart
        item {
            ConsumptionChart(
                readings = readings,
                selectedType = selectedFilter
            )
        }

        // 4. Section Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Istoric Înregistrări & Apeluri",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${filteredReadings.size} înregistrări",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 5. Readings List Items
        if (filteredReadings.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nu există înregistrări pentru filtrul selectat.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredReadings, key = { it.id }) { reading ->
                ReadingItemCard(
                    reading = reading,
                    onDelete = { onDeleteReading(reading) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ReadingItemCard(
    reading: MeterReading,
    onDelete: () -> Unit
) {
    val isGas = reading.utilityType == UtilityType.GAS
    val accentColor = if (isGas) GasCyan else ElectricityAmber
    val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("ro", "RO"))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reading_item_${reading.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Type badge + Date + Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(accentColor.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isGas) Icons.Default.LocalFireDepartment else Icons.Default.ElectricBolt,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = reading.utilityType.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = dateFormat.format(Date(reading.timestamp)),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Șterge",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body: Index, Delta, Cost
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Index Transmis",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${String.format(Locale.US, "%.0f", reading.indexValue)} ${reading.utilityType.unit}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Consum (Δ)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (reading.consumption > 0) "+${String.format(Locale.US, "%.1f", reading.consumption)} ${reading.utilityType.unit}" else "-",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Cost Estimat",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.US, "%.2f LEI", reading.estimatedCost),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = CostGreen
                    )
                }
            }

            // Footer note & call status
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (reading.notes.isNotBlank()) {
                    Text(
                        text = "“${reading.notes}”",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (reading.isCallExecuted) CostGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            tint = if (reading.isCallExecuted) CostGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (reading.isCallExecuted) "Apel IVR efectuat" else "Salvat manual",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (reading.isCallExecuted) CostGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
