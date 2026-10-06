package com.example.ui.screens

import android.Manifest
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.BuildConfig
import com.example.data.model.UtilityConfig
import com.example.data.model.UtilityType
import com.example.telecom.CallHelper
import com.example.telecom.CallLogHelper
import com.example.telecom.IvrSequenceBuilder
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
    dynamicColor: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
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

    // Test call: needs the phone permission; the utility to test is remembered while it is asked
    var pendingTestCallType by remember { mutableStateOf<UtilityType?>(null) }
    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val type = pendingTestCallType
        pendingTestCallType = null
        if (type != null) onTestCall(type, isGranted)
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

        // Card 3: Quick tests
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Teste rapide",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Apelul de test sună robotul și se oprește când cere indexul: nu transmite nimic. " +
                        "Folosește-l ca să verifici codul de client și șablonul de apel.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                for (type in UtilityType.values()) {
                    val config = if (type == UtilityType.GAS) gasConfig else electricityConfig
                    val hasClientCode = config?.clientCode?.isNotBlank() == true
                    val shortName = if (type == UtilityType.GAS) "gaz" else "curent"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onTestNotification(type) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Notificare $shortName", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                if (CallHelper.hasCallPermission(context)) {
                                    onTestCall(type, true)
                                } else {
                                    pendingTestCallType = type
                                    callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                                }
                            },
                            enabled = hasClientCode,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Apel test $shortName", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Card 4: Look. Material You takes its colours from the wallpaper (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Culori dinamice (Material You)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Folosește culorile din fundalul telefonului. Gazul rămâne albastru, curentul galben.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(checked = dynamicColor, onCheckedChange = onDynamicColorChange)
                }
            }
        }

        // Card 5: Permissions
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

        // Which build is installed (1.1.<CI run number>)
        Text(
            text = "Versiune ${BuildConfig.VERSION_NAME}",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
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
    val remindersOn = config?.isReminderEnabled ?: true
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
                        value = if (remindersOn) "Ziua $day a lunii" else "Dezactivat",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ConfigInfoTile(
                        label = "Cod Client",
                        value = code.ifBlank { "Necompletat" },
                        valueColor = if (code.isBlank()) Color(0xFFE65100) else null,
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
    modifier: Modifier = Modifier,
    valueColor: Color? = null
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
                color = valueColor ?: MaterialTheme.colorScheme.onSurface
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
    var reminderEnabled by remember { mutableStateOf(currentConfig?.isReminderEnabled ?: true) }
    var templateText by remember { mutableStateOf(currentConfig?.ivrTemplate ?: type.defaultIvrTemplate) }
    var showAdvanced by remember { mutableStateOf(false) }

    // Romanian keyboards type a decimal comma (3,02), so accept both separators
    val price = priceText.trim().replace(',', '.').toDoubleOrNull()
    val day = dayText.trim().toIntOrNull()
    val code = codeText.trim()
    val phone = phoneText.trim()
    val template = templateText.trim()

    val priceError = price == null || price <= 0.0
    val dayError = day == null || day !in 1..31
    val codeError = code.isEmpty() || !code.all { it.isDigit() }
    val phoneError = phone.length < 3 || !phone.all { it.isDigit() || it == '+' }
    val templateError = !IvrSequenceBuilder.isValidTemplate(template)
    val hasError = priceError || dayError || codeError || phoneError || templateError

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
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Preț (lei/${type.unit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = priceError,
                    supportingText = { if (priceError) Text("Introdu un preț valid, de exemplu 3,02") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = dayText,
                    onValueChange = { dayText = it },
                    label = { Text("Zi notificare lunară (1-31)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = dayError,
                    supportingText = { if (dayError) Text("Alege o zi între 1 și 31") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = codeText,
                    onValueChange = { codeText = it },
                    label = { Text("Cod Client") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = codeError,
                    supportingText = { if (codeError) Text("Codul de client conține doar cifre") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = phoneText,
                    onValueChange = { phoneText = it },
                    label = { Text("Număr TelVerde") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    isError = phoneError,
                    supportingText = { if (phoneError) Text("Introdu un număr de telefon valid") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Notificări lunare", fontSize = 14.sp)
                    Switch(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
                }

                TextButton(onClick = { showAdvanced = !showAdvanced }) {
                    Text(if (showAdvanced) "Ascunde setările avansate" else "Setări avansate (șablon apel)", fontSize = 12.sp)
                }

                // Shown automatically when the stored template is not valid, so it can be fixed
                if (showAdvanced || templateError) {
                    OutlinedTextField(
                        value = templateText,
                        onValueChange = { templateText = it },
                        label = { Text("Șablon apel") },
                        singleLine = true,
                        isError = templateError,
                        supportingText = {
                            Text(
                                if (templateError) {
                                    "Trebuie să conțină o singură dată XXXX și doar cifre, virgule, # sau *"
                                } else {
                                    "XXXX = locul indexului, virgula = pauză de 2-3 secunde"
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    TextButton(onClick = { templateText = type.ivrTemplateFor(phone, code) }) {
                        Text("Resetează șablonul la cel implicit", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!hasError && price != null && day != null) {
                        val updated = (currentConfig ?: UtilityConfig(
                            utilityType = type,
                            phoneNumber = phone,
                            clientCode = code,
                            ivrTemplate = template,
                            unitPrice = price,
                            reminderDayOfMonth = day
                        )).copy(
                            phoneNumber = phone,
                            clientCode = code,
                            ivrTemplate = template,
                            unitPrice = price,
                            reminderDayOfMonth = day,
                            isReminderEnabled = reminderEnabled
                        )
                        onSave(updated)
                    }
                },
                enabled = !hasError,
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
