package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.ui.components.IvrSequenceVisualizer
import com.example.ui.components.MetricCard
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
    notesInput: String,
    onUtilitySelected: (UtilityType) -> Unit,
    onIndexChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onTransmitAndCall: (directCall: Boolean) -> Unit,
    onSaveOnly: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Permission launcher for CALL_PHONE
    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onTransmitAndCall(true)
        } else {
            // Fallback to opening dialer
            onTransmitAndCall(false)
        }
    }

    val accentColor = if (activeUtility == UtilityType.GAS) GasCyan else ElectricityAmber
    val daysRemaining = remember(activeUtility, config) {
        val targetDay = config?.reminderDayOfMonth ?: activeUtility.defaultDay
        ReminderScheduler.getDaysUntilNextSubmission(targetDay)
    }

    val currentIndexVal = indexInput.toDoubleOrNull() ?: 0.0
    val prevIndexVal = latestReading?.indexValue ?: 0.0
    val consumption = if (currentIndexVal > 0 && prevIndexVal > 0) {
        (currentIndexVal - prevIndexVal).coerceAtLeast(0.0)
    } else 0.0

    val unitPrice = config?.unitPrice ?: activeUtility.defaultPrice
    val estimatedCost = consumption * unitPrice

    val fullDialString = remember(activeUtility, config, indexInput) {
        val template = config?.ivrTemplate ?: activeUtility.defaultIvrTemplate
        val cleanIndex = if (indexInput.isBlank()) "XXXX" else indexInput.replace(".", "")
        template.replace("XXXX", cleanIndex)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Selector Tab between GAS and ELECTRICITY
        UtilitySelectorBar(
            selectedType = activeUtility,
            onSelect = onUtilitySelected
        )

        // 2. Reminder & Submission Window Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = accentColor.copy(alpha = 0.12f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(accentColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (activeUtility == UtilityType.GAS) {
                                "Termen transmitere: 16 ale lunii"
                            } else {
                                "Termen transmitere: 24 ale lunii"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (daysRemaining == 0) {
                                "Astăzi este ziua de transmitere a indexului!"
                            } else {
                                "$daysRemaining zile rămase până la fereastra de autocitire"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (daysRemaining <= 2) CostGreen else accentColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = if (daysRemaining == 0) "AZI" else "În $daysRemaining zile",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (daysRemaining <= 2) Color.White else accentColor
                    )
                }
            }
        }

        // 3. Index Input Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Introdu Index Nou Contor",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "Unitate: ${activeUtility.unit}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Input Field
                OutlinedTextField(
                    value = indexInput,
                    onValueChange = onIndexChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("index_input_field"),
                    placeholder = {
                        Text(
                            text = if (prevIndexVal > 0) "ex: ${(prevIndexVal + 30).toInt()}" else "ex: 1234",
                            fontSize = 18.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = accentColor
                        )
                    },
                    trailingIcon = {
                        Text(
                            text = activeUtility.unit,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                // Previous Index Info
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (latestReading != null) {
                            val df = SimpleDateFormat("dd MMM yyyy", Locale("ro", "RO"))
                            "Index anterior: ${String.format(Locale.US, "%.0f", latestReading.indexValue)} ${activeUtility.unit} (${df.format(Date(latestReading.timestamp))})"
                        } else {
                            "Niciun index anterior înregistrat"
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (currentIndexVal > 0 && prevIndexVal > 0 && currentIndexVal < prevIndexVal) {
                        Text(
                            text = "Index mai mic ca anteriorul!",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Optional Notes
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = notesInput,
                    onValueChange = onNotesChanged,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Notă opțională (ex: autocitire septembrie)", fontSize = 13.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    textStyle = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // 4. Live Calculation Cards (Consumption & Estimated Cost)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Consum Calculat",
                value = if (consumption > 0) "+${String.format(Locale.US, "%.1f", consumption)} ${activeUtility.unit}" else "0 ${activeUtility.unit}",
                subtitle = "Δ Față de luna trecută",
                icon = Icons.Default.TrendingUp,
                accentColor = accentColor,
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "Cost Estimat",
                value = String.format(Locale.US, "%.2f LEI", estimatedCost),
                subtitle = "Preț: ${String.format(Locale.US, "%.2f", unitPrice)} lei/${activeUtility.unit}",
                icon = Icons.Default.Paid,
                accentColor = CostGreen,
                modifier = Modifier.weight(1f)
            )
        }

        // 5. IVR Sequence Card (Explaining the sequence, pauses, and Android call mechanics)
        IvrSequenceVisualizer(
            utilityType = activeUtility,
            fullSequence = fullDialString,
            currentIndex = indexInput,
            onOpenDialer = { onTransmitAndCall(false) }
        )

        // 6. Action Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    if (CallHelper.hasCallPermission(context)) {
                        onTransmitAndCall(true)
                    } else {
                        callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
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
                    text = "Apelează Automat & Transmite Index",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = onSaveOnly,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_reading_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Salvează doar în istoric (fără apel telefonic)", fontSize = 13.5.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
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
            // Gas Tab
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
                    Column {
                        Text(
                            text = "GAZE (16 ale lunii)",
                            fontWeight = if (isGas) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp,
                            color = if (isGas) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "TelVerde: 0800800200",
                            fontSize = 10.sp,
                            color = if (isGas) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Electricity Tab
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
                    Column {
                        Text(
                            text = "CURENT (24 ale lunii)",
                            fontWeight = if (isElec) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp,
                            color = if (isElec) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "TelVerde: 0800070701",
                            fontSize = 10.sp,
                            color = if (isElec) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}
