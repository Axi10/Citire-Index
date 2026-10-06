package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MeterReading
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// The date picker works in UTC midnight; readings keep their local time of day.
private fun toPickerMillis(timestamp: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = timestamp }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

private fun fromPickerMillis(pickerMillis: Long, original: Long): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = pickerMillis }
    return Calendar.getInstance().apply {
        timeInMillis = original
        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditReadingDialog(
    reading: MeterReading,
    onDismiss: () -> Unit,
    onSave: (MeterReading) -> Unit
) {
    var indexText by remember { mutableStateOf(reading.indexValue.toLong().toString()) }
    var timestamp by remember { mutableStateOf(reading.timestamp) }
    var showDatePicker by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale("ro", "RO")) }

    // Meter indexes are whole numbers
    val newIndex = indexText.toLongOrNull()?.toDouble()
    val prevIndex = reading.previousIndexValue
    val isBelowPrevious = newIndex != null && prevIndex != null && newIndex < prevIndex
    val isInFuture = timestamp > System.currentTimeMillis()
    val isValid = newIndex != null && newIndex > 0 && !isBelowPrevious && !isInFuture

    val newConsumption = if (newIndex != null && prevIndex != null) {
        (newIndex - prevIndex).coerceAtLeast(0.0)
    } else {
        reading.consumption
    }
    val newCost = newConsumption * reading.unitPrice

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = toPickerMillis(timestamp))
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { timestamp = fromPickerMillis(it, timestamp) }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Anulează")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Editare Index ${reading.utilityType.title}",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = indexText,
                    onValueChange = { input -> indexText = input.filter { it.isDigit() }.take(9) },
                    label = { Text("Valoare Index (${reading.utilityType.unit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = isBelowPrevious,
                    supportingText = {
                        if (isBelowPrevious) {
                            Text("Indexul nu poate fi mai mic decât cel precedent (${prevIndex?.toLong()}).")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Data citirii",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = dateFormat.format(Date(timestamp)),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isInFuture) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    OutlinedButton(onClick = { showDatePicker = true }, shape = RoundedCornerShape(8.dp)) {
                        Text("Schimbă data", fontSize = 12.sp)
                    }
                }
                if (isInFuture) {
                    Text(
                        text = "Data nu poate fi în viitor.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Index precedent: ${prevIndex?.toLong() ?: "-"} ${reading.utilityType.unit}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Consum nou calculat: ${String.format(Locale.getDefault(), "%.1f", newConsumption)} ${reading.utilityType.unit}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Cost nou estimat: ${String.format(Locale.getDefault(), "%.2f", newCost)} LEI",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newIndex != null && isValid) {
                        val updated = reading.copy(
                            indexValue = newIndex,
                            consumption = newConsumption,
                            estimatedCost = newCost,
                            timestamp = timestamp
                        )
                        onSave(updated)
                    }
                },
                enabled = isValid,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Salvează")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                Text("Anulează")
            }
        }
    )
}
