package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UtilityConfig
import com.example.data.model.UtilityType
import com.example.telecom.CallHelper
import com.example.telecom.CallLogHelper
import com.example.ui.theme.ElectricityAmber
import com.example.ui.theme.GasCyan

@Composable
fun SettingsScreen(
    gasConfig: UtilityConfig?,
    electricityConfig: UtilityConfig?,
    onSaveConfig: (UtilityConfig) -> Unit,
    onTestNotification: (UtilityType) -> Unit,
    onTestCall: (type: UtilityType, directCall: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var showAdvancedType by remember { mutableStateOf<UtilityType?>(null) }

    // System Permissions launchers
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Handled */ }

    val callLogPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Handled */ }

    val hasCallLogPermission = CallLogHelper.hasCallLogPermission(context)

    // Advanced dialog for editing IVR template and testing call up to index
    showAdvancedType?.let { type ->
        val currentCfg = if (type == UtilityType.GAS) gasConfig else electricityConfig
        AdvancedIvrSettingsDialog(
            type = type,
            currentTemplate = currentCfg?.ivrTemplate ?: type.defaultIvrTemplate,
            onDismiss = { showAdvancedType = null },
            onSaveTemplate = { newTemplate ->
                val base = currentCfg ?: UtilityConfig(
                    utilityType = type,
                    phoneNumber = type.defaultPhone,
                    clientCode = type.defaultClientCode,
                    ivrTemplate = newTemplate,
                    unitPrice = type.defaultPrice,
                    reminderDayOfMonth = type.defaultDay
                )
                onSaveConfig(base.copy(ivrTemplate = newTemplate))
                showAdvancedType = null
            },
            onTestCall = { direct ->
                onTestCall(type, direct)
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
        // System Permissions Card (Notifications & Call Log Duration)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Permisiuni Sistem",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Alerte Lunare (16 și 24)", fontSize = 12.5.sp)
                        }
                        OutlinedButton(
                            onClick = { notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Activează", fontSize = 11.sp)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(imageVector = Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Detectare Durată Apel", fontSize = 12.5.sp)
                            Text("Citește dacă apelul a durat destul", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (!hasCallLogPermission) {
                        OutlinedButton(
                            onClick = { callLogPermissionLauncher.launch(Manifest.permission.READ_CALL_LOG) },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Permite", fontSize = 11.sp)
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFE8F5E9)
                        ) {
                            Text(
                                text = "Activat",
                                color = Color(0xFF2E7D32),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Section 1: Gas Settings
        CleanUtilityCard(
            type = UtilityType.GAS,
            currentConfig = gasConfig,
            accentColor = GasCyan,
            icon = Icons.Default.LocalFireDepartment,
            onSave = onSaveConfig,
            onTestNotification = { onTestNotification(UtilityType.GAS) },
            onOpenAdvanced = { showAdvancedType = UtilityType.GAS }
        )

        // Section 2: Electricity Settings
        CleanUtilityCard(
            type = UtilityType.ELECTRICITY,
            currentConfig = electricityConfig,
            accentColor = ElectricityAmber,
            icon = Icons.Default.ElectricBolt,
            onSave = onSaveConfig,
            onTestNotification = { onTestNotification(UtilityType.ELECTRICITY) },
            onOpenAdvanced = { showAdvancedType = UtilityType.ELECTRICITY }
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun CleanUtilityCard(
    type: UtilityType,
    currentConfig: UtilityConfig?,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onSave: (UtilityConfig) -> Unit,
    onTestNotification: () -> Unit,
    onOpenAdvanced: () -> Unit
) {
    var phone by remember(currentConfig) { mutableStateOf(currentConfig?.phoneNumber ?: type.defaultPhone) }
    var clientCode by remember(currentConfig) { mutableStateOf(currentConfig?.clientCode ?: type.defaultClientCode) }
    var price by remember(currentConfig) { mutableStateOf((currentConfig?.unitPrice ?: type.defaultPrice).toString()) }
    var reminderDay by remember(currentConfig) { mutableStateOf((currentConfig?.reminderDayOfMonth ?: type.defaultDay).toString()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
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
                        Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Configurare ${type.title}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = {
                        phone = type.defaultPhone
                        clientCode = type.defaultClientCode
                        price = type.defaultPrice.toString()
                        reminderDay = type.defaultDay.toString()
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset", fontSize = 11.sp)
                }
            }

            HorizontalDivider()

            // Inputs: Client Code and Phone
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = clientCode,
                    onValueChange = { clientCode = it },
                    label = { Text("Cod Client / NLC") },
                    singleLine = true,
                    modifier = Modifier.weight(1.1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Număr TelVerde") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // Inputs: Price and Notification Day
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Preț (lei/${type.unit})") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = reminderDay,
                    onValueChange = { reminderDay = it },
                    label = { Text("Zi alertă (1-31)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onTestNotification,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test Alertă", fontSize = 11.5.sp)
                }

                Button(
                    onClick = {
                        val parsedPrice = price.toDoubleOrNull() ?: type.defaultPrice
                        val parsedDay = reminderDay.toIntOrNull()?.coerceIn(1, 31) ?: type.defaultDay
                        val updated = (currentConfig ?: UtilityConfig(
                            utilityType = type,
                            phoneNumber = phone,
                            clientCode = clientCode,
                            ivrTemplate = type.defaultIvrTemplate,
                            unitPrice = parsedPrice,
                            reminderDayOfMonth = parsedDay
                        )).copy(
                            phoneNumber = phone,
                            clientCode = clientCode,
                            unitPrice = parsedPrice,
                            reminderDayOfMonth = parsedDay
                        )
                        onSave(updated)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Salvează", fontSize = 12.sp, color = Color.White)
                }
            }

            // Discreet Advanced Options Button (Hidden unless user explicitly clicks)
            OutlinedButton(
                onClick = onOpenAdvanced,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.Build, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Opțiuni Avansate & Test Secvență Robot", fontSize = 11.5.sp)
            }
        }
    }
}

@Composable
private fun AdvancedIvrSettingsDialog(
    type: UtilityType,
    currentTemplate: String,
    onDismiss: () -> Unit,
    onSaveTemplate: (String) -> Unit,
    onTestCall: (direct: Boolean) -> Unit
) {
    var templateText by remember { mutableStateOf(currentTemplate) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Setări Avansate Robot (${type.title})",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Aici poți modifica secvența telefonică trimisă către robot (unde XXXX este indexul). Modifică doar dacă furnizorul a schimbat meniul telefonic.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = templateText,
                    onValueChange = { templateText = it },
                    label = { Text("Secvență IVR") },
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Test button up to index
                Button(
                    onClick = { onTestCall(true) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Testează Apelul (Stop la cerere index)", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveTemplate(templateText) },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Salvează")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                Text("Închide")
            }
        }
    )
}
