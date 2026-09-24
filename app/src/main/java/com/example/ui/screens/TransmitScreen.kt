package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MeterReading
import com.example.data.model.UtilityConfig
import com.example.data.model.UtilityType
import com.example.notifications.ReminderScheduler
import com.example.telecom.CallHelper
import com.example.telecom.CallStatusEvaluation
import com.example.telecom.CallVerificationResult
import com.example.ui.components.MeterScannerDialog
import com.example.ui.components.MetricCard
import com.example.ui.components.TransmitConfirmationDialog
import com.example.ui.theme.CostGreen
import com.example.ui.theme.ElectricityAmber
import com.example.ui.theme.GasCyan
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransmitScreen(
    activeUtility: UtilityType,
    config: UtilityConfig?,
    latestReading: MeterReading?,
    indexInput: String,
    callEvaluation: CallVerificationResult?,
    onUtilitySelected: (UtilityType) -> Unit,
    onIndexChanged: (String) -> Unit,
    onTransmitAndCall: (directCall: Boolean) -> Unit,
    onSaveOnly: () -> Unit,
    onUndoLastReading: () -> Unit,
    onDismissCallEvaluation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var showScannerDialog by remember { mutableStateOf(false) }
    var showConfirmationDialog by remember { mutableStateOf(false) }

    // Permission launcher for CALL_PHONE
    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onTransmitAndCall(isGranted)
    }

    val accentColor = if (activeUtility == UtilityType.GAS) GasCyan else ElectricityAmber
    val submissionStatus = remember(activeUtility, config, latestReading) {
        val targetDay = config?.reminderDayOfMonth ?: activeUtility.defaultDay
        ReminderScheduler.getSubmissionStatus(targetDay, latestReading?.timestamp)
    }

    val currentIndexVal = indexInput.toDoubleOrNull() ?: 0.0
    val prevIndexVal = latestReading?.indexValue
    val consumption = if (currentIndexVal > 0 && prevIndexVal != null) {
        (currentIndexVal - prevIndexVal).coerceAtLeast(0.0)
    } else 0.0

    val unitPrice = config?.unitPrice ?: activeUtility.defaultPrice
    val estimatedCost = consumption * unitPrice

    val isNegativeIndex = prevIndexVal != null && currentIndexVal > 0 && currentIndexVal < prevIndexVal
    val isSuspiciouslyHigh = (activeUtility == UtilityType.GAS && consumption > 350) ||
            (activeUtility == UtilityType.ELECTRICITY && consumption > 600)

    // Meter Scanner Dialog
    if (showScannerDialog) {
        MeterScannerDialog(
            onDismiss = { showScannerDialog = false },
            onIndexDetected = { detectedIndex ->
                onIndexChanged(detectedIndex)
                showScannerDialog = false
            }
        )
    }

    // Confirmation Dialog before Call
    if (showConfirmationDialog) {
        TransmitConfirmationDialog(
            type = activeUtility,
            indexValue = currentIndexVal,
            previousIndex = prevIndexVal,
            consumption = consumption,
            estimatedCost = estimatedCost,
            isSuspiciouslyHigh = isSuspiciouslyHigh,
            isNegative = isNegativeIndex,
            onDismiss = { showConfirmationDialog = false },
            onConfirm = {
                showConfirmationDialog = false
                if (CallHelper.hasCallPermission(context)) {
                    onTransmitAndCall(true)
                } else {
                    callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Selector Tab (Gas / Electricity)
        UtilitySelectorBar(
            selectedType = activeUtility,
            onSelect = onUtilitySelected
        )

        // 2. Call Verification Banner (If call was just evaluated from call log)
        if (callEvaluation != null) {
            CallOutcomeBanner(
                evaluation = callEvaluation,
                onDismiss = onDismissCallEvaluation,
                onUndo = onUndoLastReading
            )
        }

        // 3. Compact Status Bar: Period & Previous Index
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Ultimul index salvat",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (latestReading != null) "${latestReading.indexValue.toInt()} ${activeUtility.unit}" else "Nicio citire",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                val badgeColor = if (submissionStatus.isSubmittedForCurrentCycle) CostGreen else accentColor
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = submissionStatus.displayBadge,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // 4. Main Index Input Card with OCR Camera Scanner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Index Nou",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Button to open OCR camera scanner
                    Button(
                        onClick = { showScannerDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scanează", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Index Input Field with Clear & Camera affordances
                OutlinedTextField(
                    value = indexInput,
                    onValueChange = onIndexChanged,
                    label = { Text("Index (${activeUtility.unit})") },
                    placeholder = { Text("0") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = isNegativeIndex,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("meter_index_input"),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        if (indexInput.isNotEmpty()) {
                            IconButton(onClick = { onIndexChanged("") }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Șterge", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                )

                // Real-time Input Validation Warnings
                if (isNegativeIndex) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Index mai mic decât precedentul!",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (isSuspiciouslyHigh) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Consum mare (+${consumption.toInt()} ${activeUtility.unit}). Verifică!",
                            color = Color(0xFFE65100),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Live Calculation Metrics (Only when index is entered)
                if (currentIndexVal > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            title = "Consum",
                            value = "+${String.format(Locale.US, "%.1f", consumption)} ${activeUtility.unit}",
                            subtitle = if (prevIndexVal != null) "Față de ${prevIndexVal.toInt()}" else "Prima citire",
                            icon = Icons.Default.Speed,
                            accentColor = accentColor,
                            modifier = Modifier.weight(1f)
                        )

                        MetricCard(
                            title = "Cost",
                            value = String.format(Locale.US, "%.2f LEI", estimatedCost),
                            subtitle = "${String.format(Locale.US, "%.2f", unitPrice)} lei/${activeUtility.unit}",
                            icon = Icons.Default.Paid,
                            accentColor = CostGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 5. Clean, Concrete Actions
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    if (currentIndexVal > 0) {
                        showConfirmationDialog = true
                    }
                },
                enabled = currentIndexVal > 0 && !isNegativeIndex,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("call_and_transmit_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CostGreen,
                    contentColor = Color.White
                )
            ) {
                Icon(imageVector = Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Transmite (Apelează)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = onSaveOnly,
                enabled = currentIndexVal > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("save_reading_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Doar salvează", fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun CallOutcomeBanner(
    evaluation: CallVerificationResult,
    onDismiss: () -> Unit,
    onUndo: () -> Unit
) {
    val isSuccess = evaluation.status == CallStatusEvaluation.CONFIRMED_SUCCESS
    val isCancelled = evaluation.status == CallStatusEvaluation.CANCELLED_OR_MISSED

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (isSuccess) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
        border = BorderStroke(1.dp, if (isSuccess) Color(0xFF81C784) else Color(0xFFFFB74D))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isSuccess) Color(0xFF2E7D32) else Color(0xFFE65100),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSuccess) "Apel Confirmat" else "Verificare Apel",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = if (isSuccess) Color(0xFF2E7D32) else Color(0xFFE65100)
                    )
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Închide", modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = evaluation.message,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (isCancelled || evaluation.status == CallStatusEvaluation.TOO_SHORT) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onUndo,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(imageVector = Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Anulează și Șterge Indexul", fontSize = 11.5.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun UtilitySelectorBar(
    selectedType: UtilityType,
    onSelect: (UtilityType) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            val isGas = selectedType == UtilityType.GAS
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelect(UtilityType.GAS) },
                color = if (isGas) GasCyan else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = if (isGas) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GAZ (ziua 16)",
                        fontWeight = if (isGas) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.5.sp,
                        color = if (isGas) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val isElec = selectedType == UtilityType.ELECTRICITY
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelect(UtilityType.ELECTRICITY) },
                color = if (isElec) ElectricityAmber else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ElectricBolt,
                        contentDescription = null,
                        tint = if (isElec) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CURENT (ziua 24)",
                        fontWeight = if (isElec) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.5.sp,
                        color = if (isElec) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
