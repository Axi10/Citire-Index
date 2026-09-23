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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MeterReading
import java.util.Locale

@Composable
fun EditReadingDialog(
    reading: MeterReading,
    onDismiss: () -> Unit,
    onSave: (MeterReading) -> Unit
) {
    var indexText by remember { mutableStateOf(reading.indexValue.toInt().toString()) }
    var notesText by remember { mutableStateOf(reading.notes) }

    val newIndex = indexText.toDoubleOrNull()
    val prevIndex = reading.previousIndexValue
    val newConsumption = if (newIndex != null && prevIndex != null) {
        (newIndex - prevIndex).coerceAtLeast(0.0)
    } else {
        reading.consumption
    }
    val newCost = newConsumption * reading.unitPrice

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Editare Citire ${reading.utilityType.title}",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = indexText,
                    onValueChange = { indexText = it },
                    label = { Text("Valoare Index (${reading.utilityType.unit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Index precedent: ${prevIndex?.toInt() ?: "-"} ${reading.utilityType.unit}",
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

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Observații / Notă") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newIndex != null && newIndex > 0) {
                        val updated = reading.copy(
                            indexValue = newIndex,
                            consumption = newConsumption,
                            estimatedCost = newCost,
                            notes = notesText
                        )
                        onSave(updated)
                    }
                },
                enabled = newIndex != null && newIndex > 0,
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
