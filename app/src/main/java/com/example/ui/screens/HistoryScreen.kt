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
import androidx.compose.material.icons.filled.Edit
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
import com.example.ui.components.EditReadingDialog
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
    onUpdateReading: (MeterReading) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf<UtilityType?>(null) }
    var readingToEdit by remember { mutableStateOf<MeterReading?>(null) }

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

    // Edit Reading Dialog
    readingToEdit?.let { reading ->
        EditReadingDialog(
            reading = reading,
            onDismiss = { readingToEdit = null },
            onSave = { updated ->
                onUpdateReading(updated)
                readingToEdit = null
            }
        )
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
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )

                FilterChip(
                    selected = selectedFilter == UtilityType.GAS,
                    onClick = { selectedFilter = UtilityType.GAS },
                    label = { Text("Gaz") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.LocalFireDepartment, contentDescription = null, tint = GasCyan, modifier = Modifier.size(16.dp))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GasCyan.copy(alpha = 0.2f),
                        selectedLabelColor = GasCyan
                    )
                )

                FilterChip(
                    selected = selectedFilter == UtilityType.ELECTRICITY,
                    onClick = { selectedFilter = UtilityType.ELECTRICITY },
                    label = { Text("Curent") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.ElectricBolt, contentDescription = null, tint = ElectricityAmber, modifier = Modifier.size(16.dp))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricityAmber.copy(alpha = 0.2f),
                        selectedLabelColor = ElectricityAmber
                    )
                )
            }
        }

        // 2. Metrics Overview Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Cost Total",
                    value = String.format(Locale.US, "%.2f LEI", totalCost),
                    subtitle = "Gaz: ${String.format(Locale.US, "%.0f", totalGasCost)} | Curent: ${String.format(Locale.US, "%.0f", totalElecCost)}",
                    icon = Icons.Default.Paid,
                    accentColor = CostGreen,
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "Consum Total",
                    value = "${String.format(Locale.US, "%.0f", totalGasCons)} m³",
                    subtitle = "Curent: ${String.format(Locale.US, "%.0f", totalElecCons)} kWh",
                    icon = Icons.Default.TrendingUp,
                    accentColor = if (selectedFilter == UtilityType.ELECTRICITY) ElectricityAmber else GasCyan,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. Trend Visualizer Chart
        item {
            ConsumptionChart(
                readings = readings,
                selectedType = selectedFilter
            )
        }

        // 4. History List Header
        item {
            Text(
                text = "Istoric Înregistrări & Transmiteri",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (filteredReadings.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(
                        modifier = Modifier.padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nu există citiri înregistrate pentru această selecție.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredReadings, key = { it.id }) { reading ->
                ReadingItemCard(
                    reading = reading,
                    onEdit = { readingToEdit = reading },
                    onDelete = { onDeleteReading(reading) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ReadingItemCard(
    reading: MeterReading,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isGas = reading.utilityType == UtilityType.GAS
    val accentColor = if (isGas) GasCyan else ElectricityAmber
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("ro", "RO")) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reading_item_${reading.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Type, Date, Edit & Delete buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(accentColor.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isGas) Icons.Default.LocalFireDepartment else Icons.Default.ElectricBolt,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editează",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(2.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Șterge",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
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
                        fontSize = 17.sp,
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
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = CostGreen
                    )
                }
            }

            // Optional notes or transmission tag
            if (reading.notes.isNotBlank() || reading.isCallExecuted) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (reading.notes.isNotBlank()) {
                        Text(
                            text = reading.notes,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (reading.isCallExecuted) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = CostGreen.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = null,
                                    tint = CostGreen,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Apel realizat",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = CostGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
