package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UtilityConfig
import com.example.data.model.UtilityType
import com.example.telecom.CallHelper
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Notification permission launcher for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Permission handled */ }

    // Phone call permission launcher for direct test calls
    var pendingTestUtility by remember { mutableStateOf<UtilityType?>(null) }
    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        pendingTestUtility?.let { type ->
            onTestCall(type, isGranted)
            pendingTestUtility = null
        }
    }

    val launchTestCall: (UtilityType, Boolean) -> Unit = { type, direct ->
        if (direct) {
            if (CallHelper.hasCallPermission(context)) {
                onTestCall(type, true)
            } else {
                pendingTestUtility = type
                callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
            }
        } else {
            onTestCall(type, false)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Notification permission banner if on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Permisiune Notificări Lunare",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Activează notificările pentru a fi avertizat pe 16 (Gaz) și 24 (Curent).",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Activează", fontSize = 11.sp)
                    }
                }
            }
        }

        // =========================================================================
        // 🧪 SPECIAL TEST CALL SECTION: STOPS RIGHT BEFORE ENTERING THE METER INDEX
        // =========================================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("test_call_container_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            ),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneInTalk,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "🧪 Testare Apel Robotic (Fără Transmitere)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Se oprește exact la pasul de introducere index",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text(
                    text = "Apasă butonul de test pentru a verifica apelarea și navigarea prin robot. Secvența rulează identic (număr, meniuri, cod client, pauze), dar se oprește exact când robotul solicită indexul contorului. Ascultă în apel dacă robotul îți cere indexul!",
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 1. GAZ TEST CALL CARD
                val gasTemplate = gasConfig?.ivrTemplate ?: UtilityType.GAS.defaultIvrTemplate
                val gasTestSeq = IvrSequenceBuilder.buildTestSequenceUntilIndex(gasTemplate)
                TestCallUtilityBox(
                    type = UtilityType.GAS,
                    accentColor = GasCyan,
                    icon = Icons.Default.LocalFireDepartment,
                    testSequence = gasTestSeq,
                    explanation = "Apelează 0800800200 -> trimite cod client ${gasConfig?.clientCode ?: UtilityType.GAS.defaultClientCode}# -> pauză -> se oprește la solicitarea indexului.",
                    onDirectTest = { launchTestCall(UtilityType.GAS, true) },
                    onDialerTest = { launchTestCall(UtilityType.GAS, false) }
                )

                // 2. ELECTRICITY TEST CALL CARD
                val elecTemplate = electricityConfig?.ivrTemplate ?: UtilityType.ELECTRICITY.defaultIvrTemplate
                val elecTestSeq = IvrSequenceBuilder.buildTestSequenceUntilIndex(elecTemplate)
                TestCallUtilityBox(
                    type = UtilityType.ELECTRICITY,
                    accentColor = ElectricityAmber,
                    icon = Icons.Default.ElectricBolt,
                    testSequence = elecTestSeq,
                    explanation = "Apelează 0800070701 -> tasta 1 -> cod client ${electricityConfig?.clientCode ?: UtilityType.ELECTRICITY.defaultClientCode}# -> tasta 1 -> pauză -> se oprește la solicitarea indexului.",
                    onDirectTest = { launchTestCall(UtilityType.ELECTRICITY, true) },
                    onDialerTest = { launchTestCall(UtilityType.ELECTRICITY, false) }
                )
            }
        }

        // Section 1: Gas Settings
        UtilityConfigEditorCard(
            type = UtilityType.GAS,
            currentConfig = gasConfig,
            accentColor = GasCyan,
            icon = Icons.Default.LocalFireDepartment,
            onSave = onSaveConfig,
            onTestNotification = { onTestNotification(UtilityType.GAS) },
            onTestCall = { direct -> launchTestCall(UtilityType.GAS, direct) }
        )

        // Section 2: Electricity Settings
        UtilityConfigEditorCard(
            type = UtilityType.ELECTRICITY,
            currentConfig = electricityConfig,
            accentColor = ElectricityAmber,
            icon = Icons.Default.ElectricBolt,
            onSave = onSaveConfig,
            onTestNotification = { onTestNotification(UtilityType.ELECTRICITY) },
            onTestCall = { direct -> launchTestCall(UtilityType.ELECTRICITY, direct) }
        )

        // Section 3: Technical Architecture Explanation
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Arhitectură Apel Automat & Permisiuni",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "• CALL_PHONE vs ACTION_DIAL: Aplicația folosește permisiunea standard Android 'android.permission.CALL_PHONE' pentru a declanșa apelul imediat la apăsarea butonului, fără să mai fie nevoie să deschizi tastatura și să apeși tu butonul 'Suna'.\n\n" +
                            "• Pauze și tonuri DTMF: În secvența telecom, virgula (,) reprezintă o pauză de ~2.5 secunde. Modemul celular transmite în mod autonom codul clientului (#), indexul contorului (#) și tastele de confirmare (1).\n\n" +
                            "• De ce apare ecranul de apel? Sistemul de operare Android garantează securitatea utilizatorului afișând interfața nativă în timpul oricărei convorbiri celulare. Nicio aplicație nu poate efectua apeluri complet ascunse fără ca utilizatorul să vadă că este conectat, însă întregul proces de tastare a indexului este 100% automatizat!",
                    fontSize = 11.5.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun TestCallUtilityBox(
    type: UtilityType,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    testSequence: String,
    explanation: String,
    onDirectTest: () -> Unit,
    onDialerTest: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(accentColor.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Test Apel: ${type.title}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accentColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Stop la index",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Sequence chip
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Secvență:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = testSequence,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Text(
                text = explanation,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onDirectTest,
                    modifier = Modifier
                        .weight(1.3f)
                        .height(44.dp)
                        .testTag("test_call_direct_${type.name.lowercase()}"),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "📞 Sună Acum (Test)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                OutlinedButton(
                    onClick = onDialerTest,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Dialpad, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tastatură", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun UtilityConfigEditorCard(
    type: UtilityType,
    currentConfig: UtilityConfig?,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onSave: (UtilityConfig) -> Unit,
    onTestNotification: () -> Unit,
    onTestCall: (direct: Boolean) -> Unit
) {
    var phone by remember(currentConfig) { mutableStateOf(currentConfig?.phoneNumber ?: type.defaultPhone) }
    var clientCode by remember(currentConfig) { mutableStateOf(currentConfig?.clientCode ?: type.defaultClientCode) }
    var ivrTemplate by remember(currentConfig) { mutableStateOf(currentConfig?.ivrTemplate ?: type.defaultIvrTemplate) }
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
                        ivrTemplate = type.defaultIvrTemplate
                        price = type.defaultPrice.toString()
                        reminderDay = type.defaultDay.toString()
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Implicit", fontSize = 11.sp)
                }
            }

            HorizontalDivider()

            // Phone and Client Code
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Număr TelVerde") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = clientCode,
                    onValueChange = { clientCode = it },
                    label = { Text("Cod Client / NLC") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // IVR Dial Template
            OutlinedTextField(
                value = ivrTemplate,
                onValueChange = { ivrTemplate = it },
                label = { Text("Secvență apel IVR (folosește XXXX pentru index)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                shape = RoundedCornerShape(10.dp)
            )

            // Price and Reminder Day
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
                    label = { Text("Zi notificare (1-31)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // Buttons: Test Notification & Save Config
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onTestNotification,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Testează Alerta", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        val parsedPrice = price.toDoubleOrNull() ?: type.defaultPrice
                        val parsedDay = reminderDay.toIntOrNull()?.coerceIn(1, 31) ?: type.defaultDay
                        onSave(
                            UtilityConfig(
                                utilityType = type,
                                phoneNumber = phone,
                                clientCode = clientCode,
                                ivrTemplate = ivrTemplate,
                                unitPrice = parsedPrice,
                                reminderDayOfMonth = parsedDay
                            )
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Salvează", fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }
}
