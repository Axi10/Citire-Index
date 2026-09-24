package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.UtilityConfig
import com.example.data.model.UtilityType
import com.example.telecom.CallLogHelper
import com.example.ui.theme.CostGreen
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
    val lifecycleOwner = LocalLifecycleOwner.current
    val scrollState = rememberScrollState()

    var configToEdit by remember { mutableStateOf<UtilityType?>(null) }

    // Dynamic permission states
    var hasNotifPermission by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
    var hasCallLogPermission by remember {
        mutableStateOf(CallLogHelper.hasCallLogPermission(context))
    }

    // Auto-refresh permission status whenever user returns to the app
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotifPermission = NotificationManagerCompat.from(context).areNotificationsEnabled()
                hasCallLogPermission = CallLogHelper.hasCallLogPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotifPermission = isGranted || NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    val callLogPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCallLogPermission = isGranted || CallLogHelper.hasCallLogPermission(context)
    }

    // Edit Dialog (Only opens when user explicitly taps "Modifică")
    configToEdit?.let { type ->
        val currentCfg = if (type == UtilityType.GAS) gasConfig else electricityConfig
        EditConfigModal(
            type = type,
            currentConfig = currentCfg,
            onDismiss = { configToEdit = null },
            onSave = { updated ->
                onSaveConfig(updated)
                configToEdit = null
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Card 1: Gas Summary Card
        MinimalUtilityCard(
            type = UtilityType.GAS,
            config = gasConfig,
            accentColor = GasCyan,
            icon = Icons.Default.LocalFireDepartment,
            onEdit = { configToEdit = UtilityType.GAS }
        )

        // Card 2: Electricity Summary Card
        MinimalUtilityCard(
            type = UtilityType.ELECTRICITY,
            config = electricityConfig,
            accentColor = ElectricityAmber,
            icon = Icons.Default.ElectricBolt,
            onEdit = { configToEdit = UtilityType.ELECTRICITY }
        )

        // Card 3: Permissions
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Permisiuni & Servicii Sistem",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                // 1. Notification Permission Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Alerte lunare", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("Notificări transmitere index", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    if (hasNotifPermission) {
                        Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFE8F5E9)) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = CostGreen,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Activat",
                                    color = CostGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        val appIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.fromParts("package", context.packageName, null)
                                        }
                                        context.startActivity(appIntent)
                                    }
                                }
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Activează", fontSize = 11.sp)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // 2. Call Log Permission Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Detectare durată apel", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("Validare automată convorbire", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFE8F5E9)) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = CostGreen,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Activat",
                                    color = CostGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MinimalUtilityCard(
    type: UtilityType,
    config: UtilityConfig?,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onEdit: () -> Unit
) {
    val price = config?.unitPrice ?: type.defaultPrice
    val day = config?.reminderDayOfMonth ?: type.defaultDay
    val code = config?.clientCode ?: type.defaultClientCode
    val phone = config?.phoneNumber ?: type.defaultPhone

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
                        text = type.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Modifică", fontSize = 11.5.sp)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Clean 2x2 Grid of details so nothing is cramped
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ConfigInfoTile(
                        label = "Preț unitar",
                        value = "$price lei/${type.unit}",
                        modifier = Modifier.weight(1f)
                    )
                    ConfigInfoTile(
                        label = "Zi notificare",
                        value = "Ziua $day a lunii",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ConfigInfoTile(
                        label = "Cod Client",
                        value = code,
                        modifier = Modifier.weight(1f)
                    )
                    ConfigInfoTile(
                        label = "TelVerde IVR",
                        value = phone,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfigInfoTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun EditConfigModal(
    type: UtilityType,
    currentConfig: UtilityConfig?,
    onDismiss: () -> Unit,
    onSave: (UtilityConfig) -> Unit
) {
    var priceText by remember { mutableStateOf((currentConfig?.unitPrice ?: type.defaultPrice).toString()) }
    var dayText by remember { mutableStateOf((currentConfig?.reminderDayOfMonth ?: type.defaultDay).toString()) }
    var codeText by remember { mutableStateOf(currentConfig?.clientCode ?: type.defaultClientCode) }
    var phoneText by remember { mutableStateOf(currentConfig?.phoneNumber ?: type.defaultPhone) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Modificare Setări ${type.title}",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Preț (lei/${type.unit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = dayText,
                    onValueChange = { dayText = it },
                    label = { Text("Zi notificare lunară (1-31)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = codeText,
                    onValueChange = { codeText = it },
                    label = { Text("Cod Client") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = phoneText,
                    onValueChange = { phoneText = it },
                    label = { Text("Număr TelVerde") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = priceText.toDoubleOrNull() ?: type.defaultPrice
                    val d = dayText.toIntOrNull()?.coerceIn(1, 31) ?: type.defaultDay
                    val updated = (currentConfig ?: UtilityConfig(
                        utilityType = type,
                        phoneNumber = phoneText,
                        clientCode = codeText,
                        ivrTemplate = type.defaultIvrTemplate,
                        unitPrice = p,
                        reminderDayOfMonth = d
                    )).copy(
                        phoneNumber = phoneText,
                        clientCode = codeText,
                        unitPrice = p,
                        reminderDayOfMonth = d
                    )
                    onSave(updated)
                },
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
