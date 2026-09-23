package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.UtilityType
import com.example.notifications.ReminderNotificationReceiver
import com.example.notifications.ReminderScheduler
import com.example.ui.MainViewModel
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.PdfReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransmitScreen
import com.example.ui.theme.ElectricityAmber
import com.example.ui.theme.GasCyan
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Ensure notification channel is registered & reminders scheduled
        ReminderNotificationReceiver.ensureChannel(this)
        ReminderScheduler.rescheduleAll(this)

        val notificationUtility = intent.getStringExtra(ReminderNotificationReceiver.EXTRA_UTILITY_TYPE)

        setContent {
            MyApplicationTheme {
                val viewModel: MainViewModel = viewModel()

                // If launched from notification, activate that utility tab
                LaunchedEffect(notificationUtility) {
                    if (notificationUtility == UtilityType.ELECTRICITY.name) {
                        viewModel.selectUtility(UtilityType.ELECTRICITY)
                        viewModel.setTab(0)
                    } else if (notificationUtility == UtilityType.GAS.name) {
                        viewModel.selectUtility(UtilityType.GAS)
                        viewModel.setTab(0)
                    }
                }

                MainScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val activeUtility by viewModel.activeUtility.collectAsStateWithLifecycle()
    val currentConfig by viewModel.currentConfig.collectAsStateWithLifecycle()
    val currentLatestReading by viewModel.currentLatestReading.collectAsStateWithLifecycle()
    val indexInput by viewModel.indexInput.collectAsStateWithLifecycle()
    val notesInput by viewModel.notesInput.collectAsStateWithLifecycle()
    val allReadings by viewModel.allReadings.collectAsStateWithLifecycle()
    val gasConfig by viewModel.gasConfig.collectAsStateWithLifecycle()
    val electricityConfig by viewModel.electricityConfig.collectAsStateWithLifecycle()
    val lastGeneratedPdf by viewModel.generatedPdf.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val callEvaluation by viewModel.lastCallEvaluation.collectAsStateWithLifecycle()

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.verifyCallOutcome(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    val topBarTitle = when (selectedTab) {
        0 -> "Index Utilități — Transmitere"
        1 -> "Istoric & Statistici Consum"
        2 -> "Rapoarte PDF & Evidență"
        3 -> "Configurare IVR & Notificări"
        else -> "Index Utilități"
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = topBarTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { viewModel.setTab(0) },
                    icon = { Icon(Icons.Default.PhoneInTalk, contentDescription = "Transmitere") },
                    label = { Text("Transmitere", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = if (activeUtility == UtilityType.GAS) GasCyan.copy(alpha = 0.2f) else ElectricityAmber.copy(alpha = 0.2f)
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { viewModel.setTab(1) },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Istoric") },
                    label = { Text("Istoric", fontSize = 11.sp) }
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { viewModel.setTab(2) },
                    icon = { Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF") },
                    label = { Text("Raport PDF", fontSize = 11.sp) }
                )

                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { viewModel.setTab(3) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Setări") },
                    label = { Text("Setări", fontSize = 11.sp) }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> TransmitScreen(
                    activeUtility = activeUtility,
                    config = currentConfig,
                    latestReading = currentLatestReading,
                    indexInput = indexInput,
                    notesInput = notesInput,
                    callEvaluation = callEvaluation,
                    onUtilitySelected = { viewModel.selectUtility(it) },
                    onIndexChanged = { viewModel.updateIndexInput(it) },
                    onNotesChanged = { viewModel.updateNotesInput(it) },
                    onTransmitAndCall = { directCall -> viewModel.transmitAndCall(context, directCall) },
                    onSaveOnly = { viewModel.saveReadingOnly() },
                    onUndoLastReading = { viewModel.undoLastReading() },
                    onDismissCallEvaluation = { viewModel.clearLastCallEvaluation() }
                )

                1 -> HistoryScreen(
                    readings = allReadings,
                    onDeleteReading = { viewModel.deleteReading(it) },
                    onUpdateReading = { viewModel.updateReading(it) }
                )

                2 -> PdfReportsScreen(
                    readings = allReadings,
                    lastGeneratedFile = lastGeneratedPdf,
                    onExportPdf = { periodName -> viewModel.exportPdfReport(context, periodName) }
                )

                3 -> SettingsScreen(
                    gasConfig = gasConfig,
                    electricityConfig = electricityConfig,
                    onSaveConfig = { viewModel.updateConfig(it) },
                    onTestNotification = { type -> viewModel.triggerTestNotification(context, type) },
                    onTestCall = { type, directCall -> viewModel.triggerTestCall(context, type, directCall) }
                )
            }
        }
    }
}

/**
 * Backward compatibility for GreetingScreenshotTest and existing test suite.
 */
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
