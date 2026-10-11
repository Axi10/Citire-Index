package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import com.example.ui.components.TransmitConfirmationDialog
import com.example.ui.theme.CostGreen
import com.example.ui.theme.ElectricityAmber
import com.example.ui.theme.GasCyan
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Gradient of the main card for each meter: dark enough for white text
private val GasGradient = listOf(Color(0xFF075985), Color(0xFF0284C7))
private val ElectricityGradient = listOf(Color(0xFFB45309), Color(0xFFD97706))

@Composable
fun TransmitScreen(
    activeUtility: UtilityType,
    config: UtilityConfig?,
    latestReading: MeterReading?,
    lastTransmittedAt: Long?,
    indexInput: String,
    callEvaluation: CallVerificationResult?,
    onUtilitySelected: (UtilityType) -> Unit,
    onIndexChanged: (String) -> Unit,
    onTransmitAndCall: (directCall: Boolean) -> Unit,
    onSaveOnly: () -> Unit,
    onUndoLastReading: () -> Unit,
    onDismissCallEvaluation: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager: FocusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    var showScannerDialog by remember { mutableStateOf(false) }
    var showConfirmationDialog by remember { mutableStateOf(false) }

    // Phone permission for the automatic call; the call log permission is asked at the same time
    // so the app can later check how long the call lasted (the user may refuse it).
    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        onTransmitAndCall(result[Manifest.permission.CALL_PHONE] == true)
    }

    val accentColor = if (activeUtility == UtilityType.GAS) GasCyan else ElectricityAmber
    val gradient = if (activeUtility == UtilityType.GAS) GasGradient else ElectricityGradient

    // Only a reading whose call really started counts as "sent"; one that was just saved does not
    val submissionStatus = remember(activeUtility, config, lastTransmittedAt) {
        val targetDay = config?.reminderDayOfMonth ?: activeUtility.defaultDay
        ReminderScheduler.getSubmissionStatus(targetDay, lastTransmittedAt)
    }

    // Config is null only while it is loading; an empty client code means the user must set it up
    val needsSetup = config != null && config.clientCode.isBlank()

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

    val lastIndexDate = remember(latestReading?.timestamp) {
        latestReading?.let { SimpleDateFormat("d MMM", Locale.forLanguageTag("ro-RO")).format(Date(it.timestamp)) }
    }

    // Meter Scanner Dialog. The scanner gets the previous index and a plausible jump (a few months
    // of consumption), so it can tell the real index from serial numbers and red decimals.
    if (showScannerDialog) {
        MeterScannerDialog(
            onDismiss = { showScannerDialog = false },
            onIndexDetected = { detectedIndex ->
                onIndexChanged(detectedIndex)
                showScannerDialog = false
            },
            previousIndex = prevIndexVal?.toLong(),
            maxJump = if (activeUtility == UtilityType.GAS) 600L else 1000L
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
                    callPermissionLauncher.launch(
                        arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_CALL_LOG)
                    )
                }
            },
            alreadySubmitted = submissionStatus.isSubmittedForCurrentCycle
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        UtilitySelectorBar(
            selectedType = activeUtility,
            onSelect = onUtilitySelected
        )

        // Without the client code the provider's robot cannot identify the customer
        if (needsSetup) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenSettings),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFFFF3E0),
                border = BorderStroke(1.dp, Color(0xFFFFB74D))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Adaugă codul de client în Setări",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color(0xFF4E342E),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Deschide",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFFE65100)
                    )
                }
            }
        }

        if (callEvaluation != null) {
            CallOutcomeBanner(
                evaluation = callEvaluation,
                onDismiss = onDismissCallEvaluation,
                onUndo = onUndoLastReading
            )
        }

        // Hero card: the index is typed straight into the big number
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = Color.Transparent,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .background(Brush.linearGradient(gradient))
                    .padding(horizontal = 22.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "INDEX NOU",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )

                    Surface(
                        onClick = { showScannerDialog = true },
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Scanează",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    BasicTextField(
                        value = indexInput,
                        // Indexes are whole numbers: anything else typed or pasted is dropped
                        onValueChange = { onIndexChanged(it.filter { c -> c.isDigit() }.take(9)) },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 54.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-1).sp
                        ),
                        cursorBrush = SolidColor(Color.White),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("meter_index_input"),
                        decorationBox = { inner ->
                            Box {
                                if (indexInput.isEmpty()) {
                                    Text(
                                        text = "0000",
                                        color = Color.White.copy(alpha = 0.3f),
                                        fontSize = 54.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = (-1).sp
                                    )
                                }
                                inner()
                            }
                        }
                    )

                    if (indexInput.isNotEmpty()) {
                        IconButton(onClick = { onIndexChanged("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Șterge",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Text(
                        text = activeUtility.unit,
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 6.dp, bottom = 12.dp)
                    )
                }

                // One line: previous index, then consumption and cost once a number is typed
                Text(
                    text = if (latestReading != null) {
                        "Ultimul: ${latestReading.indexValue.toLong()}  ·  $lastIndexDate"
                    } else {
                        "Prima citire"
                    },
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )

                if (isNegativeIndex) {
                    HeroNote(text = "Mai mic decât ultimul index", icon = Icons.Default.Warning)
                } else if (currentIndexVal > 0 && prevIndexVal != null) {
                    val costText = String.format(Locale.US, "%.2f", estimatedCost)
                    HeroNote(
                        text = "+${consumption.toLong()} ${activeUtility.unit}  ·  $costText lei" +
                            if (isSuspiciouslyHigh) "  ·  verifică" else "",
                        icon = if (isSuspiciouslyHigh) Icons.Default.Warning else null
                    )
                }
            }
        }

        // Submission status for the current month
        val badgeColor = if (submissionStatus.isSubmittedForCurrentCycle) CostGreen else accentColor
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(badgeColor)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = submissionStatus.displayBadge,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(
                onClick = {
                    if (currentIndexVal > 0) {
                        focusManager.clearFocus()
                        showConfirmationDialog = true
                    }
                },
                enabled = currentIndexVal > 0 && !isNegativeIndex && !needsSetup,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .testTag("call_and_transmit_button"),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CostGreen,
                    contentColor = Color.White
                )
            ) {
                Icon(imageVector = Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Transmite",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            TextButton(
                onClick = onSaveOnly,
                enabled = currentIndexVal > 0 && !isNegativeIndex,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_reading_button")
            ) {
                Text("Doar salvează", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

/** A small pill under the big number (consumption, cost, warnings). */
@Composable
private fun HeroNote(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector?) {
    Surface(
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.22f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
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

    // The banner has a fixed light background, so its text must be dark in every theme
    val bodyTextColor = Color(0xFF1F2937)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = if (isSuccess) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
        border = BorderStroke(1.dp, if (isSuccess) Color(0xFF81C784) else Color(0xFFFFB74D))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isSuccess) "Apel finalizat" else "Verifică apelul",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isSuccess) Color(0xFF2E7D32) else Color(0xFFE65100)
                    )
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Închide",
                        tint = bodyTextColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = evaluation.message,
                fontSize = 13.sp,
                color = bodyTextColor
            )

            if (isCancelled || evaluation.status == CallStatusEvaluation.TOO_SHORT) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onUndo,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(imageVector = Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Șterge indexul", fontSize = 12.sp)
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
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            UtilityTab(
                label = "GAZ",
                icon = Icons.Default.LocalFireDepartment,
                selected = selectedType == UtilityType.GAS,
                color = GasCyan,
                onClick = { onSelect(UtilityType.GAS) },
                modifier = Modifier.weight(1f)
            )
            UtilityTab(
                label = "CURENT",
                icon = Icons.Default.ElectricBolt,
                selected = selectedType == UtilityType.ELECTRICITY,
                color = Color(0xFFD97706),
                onClick = { onSelect(UtilityType.ELECTRICITY) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun UtilityTab(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = if (selected) color else Color.Transparent,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 13.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
